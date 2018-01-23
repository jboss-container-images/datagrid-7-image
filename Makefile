DEV_IMAGE_ORG = jboss-dataservices
DOCKER_REGISTRY_ENGINEERING =
DOCKER_REGISTRY_REDHAT =
DEV_IMAGE_NAME = datagrid-online-services-dev
ADDITIONAL_ARGUMENTS =

CE_DOCKER = $(shell docker version | grep Version | head -n 1 | grep -e "-ce")
ifneq ($(CE_DOCKER),)
	DOCKER_REGISTRY_ENGINEERING = docker-registry.engineering.redhat.com
	DOCKER_REGISTRY_REDHAT = registry.access.redhat.com/
	DEV_IMAGE_FULL_NAME = $(DOCKER_REGISTRY_ENGINEERING)/$(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME)
	CONCREATE_CMD = concreate generate --overrides=overrides.yaml --target target-docker;
else
	DEV_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME)
	CONCREATE_CMD = concreate generate --target target-docker;
endif

# In order to test this image we need to do a little trick. The APB image is pushed under the following name:
# http://$REGISTRY:5000/myproject/datagrid-online-services-apb
# Since the project name (myproject) and image name (datagrid-online-services-apb) match
# OpenShift "thinks" that this image has already been pulled from some registry.
# But the reality is different - we pushed it...
DEV_APB_IMAGE_NAME = datagrid-online-services-apb
DEV_APB_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_APB_IMAGE_NAME)

MVN_COMMAND = mvn

# You may replace it with your custom command. See https://github.com/ansibleplaybookbundle/ansible-playbook-bundle#installing-the-apb-tool
APB_COMMAND = docker run --rm --privileged -v `pwd`:/mnt -v ${HOME}/.kube:/.kube -v /var/run/docker.sock:/var/run/docker.sock -u `id -u` docker.io/ansibleplaybookbundle/apb

_TEST_PROJECT = myproject
_REGISTRY_IP = $(shell oc get svc/docker-registry -n default -o yaml | grep 'clusterIP:' | awk '{print $$2}')
_IMAGE = $(_REGISTRY_IP):5000/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME)
_APB_IMAGE = $(_REGISTRY_IP):5000/$(_TEST_PROJECT)/$(DEV_APB_IMAGE_NAME)
# This username and password is hardcoded (and base64 encoded) in the Ansible
# Service Broker template
_ANSIBLE_SERVICE_BROKER_USERNAME = admin
_ANSIBLE_SERVICE_BROKER_PASSWORD = admin

start-openshift-with-catalog:
	@echo "---- Starting OpenShift ----"
	oc cluster up --service-catalog

	@echo "---- Granting admin rights to Developer ----"
	oc login -u system:admin
	oc adm policy add-cluster-role-to-user cluster-admin developer
	oc login -u developer -p developer

	@echo "---- Switching to test project ----"
	oc project $(_TEST_PROJECT)

	@echo "---- Allowing containers to run specific users ----"
	# Some of the JDK commands (jcmd, jps etc.) require the same user as the one running java process.
	# The command below enabled that. The process inside the container will be ran using jboss user.
	# The same users will be used by default for `oc rsh` command.
	oc adm policy add-scc-to-group anyuid system:authenticated
.PHONY: start-openshift-with-catalog

start-openshift-with-catalog-and-ansible-service-broker: start-openshift-with-catalog install-ansible-service-broker
.PHONY: start-openshift-with-catalog-and-ansible-service-broker

install-ansible-service-broker:
	@echo "---- Installing Ansible Service Broker ----"
	oc new-project ansible-service-broker
	( \
		curl -s https://raw.githubusercontent.com/openshift/ansible-service-broker/master/templates/deploy-ansible-service-broker.template.yaml \
        | oc process \
        -n ansible-service-broker \
        -p BROKER_KIND="Broker" \
        -p BROKER_AUTH="{\"basicAuthSecret\":{\"namespace\":\"ansible-service-broker\",\"name\":\"asb-auth-secret\"}}" \
        -p ENABLE_BASIC_AUTH="true" -f - | oc create -f - \
	)
.PHONY: install-ansible-service-broker

stop-openshift:
	oc cluster down
.PHONY: stop-openshift

build-image:
	( \
		virtualenv ~/concreate; \
		source ~/concreate/bin/activate; \
		pip install -U concreate==1.3.4; \
		$(CONCREATE_CMD) \
		deactivate; \
	)
	sudo docker build --force-rm -t $(DEV_IMAGE_FULL_NAME) ./target-docker/image
.PHONY: build-image

_login_to_openshift:
	( \
		until oc get pod -n default | grep docker-registry | grep "1/1" > /dev/null; do \
			sleep 10; \
			echo "Waiting for Docker Registry..."; \
		done; \
	)
	sudo docker login -u $(shell oc whoami) -p $(shell oc whoami -t) $(_REGISTRY_IP):5000
.PHONY: _login_to_openshift

_add_openshift_push_permissions:
	oc adm policy add-role-to-user system:registry developer || true
	oc adm policy add-role-to-user admin developer -n ${_TEST_PROJECT} || true
	oc adm policy add-role-to-user system:image-builder developer || true
.PHONY: _add_openshift_push_permissions

push-image-to-local-openshift: _add_openshift_push_permissions _login_to_openshift
	sudo docker tag $(DEV_IMAGE_FULL_NAME) $(_IMAGE)
	sudo docker push $(_IMAGE)
.PHONY: push-image-to-local-openshift

test-functional:
	$(MVN_COMMAND) -Dimage=$(_IMAGE) -Dkubernetes.auth.token=$(shell oc whoami -t) -DDOCKER_REGISTRY_REDHAT=$(DOCKER_REGISTRY_REDHAT) clean test -f functional-tests/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-functional

test-unit:
	$(MVN_COMMAND) clean test -f modules/os-datagrid-online-services-configuration/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-unit

_relist-template-service-broker:
	# This one is very hacky - the idea is to increase the relist request counter by 1. This way we ask the Template
	# Service Broker to refresh all templates. The rest of the complication is due to how Makefile parses file.
	RELIST_TO_BE_SET=`expr $(shell oc get ClusterServiceBroker/template-service-broker --template={{.spec.relistRequests}}) + 1` && \
	oc patch ClusterServiceBroker/template-service-broker -p '{"spec":{"relistRequests": '$$RELIST_TO_BE_SET'}}'
.PHONY: _relist-template-service-broker

install-templates-in-openshift-namespace: _relist-template-service-broker
	oc create -f templates/caching-service.json -n openshift || true
	oc create -f templates/shared-memory-service.json -n openshift || true
.PHONY: install-templates-in-openshift-namespace

install-templates:
	oc create -f templates/caching-service.json || true
	oc create -f templates/shared-memory-service.json || true
.PHONY: install-templates

clear-templates:
	oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
	oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
	oc delete template caching-service || true
	oc delete template shared-memory-service || true
.PHONY: clear-templates

test-caching-service-manually:
	oc process caching-service -p NAMESPACE=$(shell oc project -q) -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_IMAGE) -p KEYSTORE_PASSWORD=test99 | oc create -f -
	oc expose svc/caching-service-app-http || true
	oc expose svc/caching-service-app-hotrod || true
	oc get routes
.PHONY: test-caching-service-manually

test-shared-memory-service-manually:
	oc process shared-memory-service -p NAMESPACE=$(shell oc project -q) -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_IMAGE)  -p KEYSTORE_PASSWORD=test99 | oc create -f -
	oc expose svc/shared-memory-service-app-http || true
	oc expose svc/shared-memory-service-app-hotrod || true
	oc get routes
.PHONY: test-shared-memory-service-manually

clean-maven:
	$(MVN_COMMAND) clean -f modules/os-datagrid-online-services-configuration/pom.xml || true
	$(MVN_COMMAND) clean -f functional-tests/pom.xml || true
.PHONY: clean-maven

clean-docker:
	sudo docker rmi $(_IMAGE) || true
	sudo docker rmi $(_APB_IMAGE) || true
	rm -rf target-docker
.PHONY: clean-docker

clean: clean-docker clean-maven stop-openshift
.PHONY: clean

test-ci: clean test-unit start-openshift-with-catalog build-image push-image-to-local-openshift test-functional
.PHONY: test-ci

clean-ci: clean-docker stop-openshift #avoid cleaning Maven as we need results to be reported by the job
.PHONY: clean-ci

apb-build:
	(\
		cd service-broker/datagrid-online-services-apb; \
		$(APB_COMMAND) prepare; \
		sudo docker build --force-rm -t $(DEV_APB_IMAGE_FULL_NAME) ./; \
	)
.PHONY: apb-build

_add_apb_roles:
	oc policy add-role-to-user cluster-admin system:serviceaccount:myproject:default -n myproject || true
.PHONY: _add_apb_roles

_wait_for_ansible_service_broker:
	( \
		until oc get pods -n ansible-service-broker | grep asb | grep "2/2" > /dev/null; do \
			sleep 20; \
			echo "Waiting for Ansible Service Broker..."; \
		done; \
	)
.PHONY: _wait_for_ansible_service_broker

apb-push-to-local-broker: _add_openshift_push_permissions _add_apb_roles apb-build _login_to_openshift _wait_for_ansible_service_broker
	(\
		cd service-broker/datagrid-online-services-apb; \
		$(APB_COMMAND) push -u $(_ANSIBLE_SERVICE_BROKER_USERNAME) -p $(_ANSIBLE_SERVICE_BROKER_PASSWORD); \
		$(APB_COMMAND) list -u $(_ANSIBLE_SERVICE_BROKER_USERNAME) -p $(_ANSIBLE_SERVICE_BROKER_PASSWORD); \
	)
	sudo docker tag $(DEV_APB_IMAGE_FULL_NAME) $(_APB_IMAGE)
	sudo docker push $(_APB_IMAGE)
.PHONY: apb-push-to-local-broker

test-apb-provision: apb-push-to-local-broker
	# This needs to be called twice :(
	# https://github.com/ansibleplaybookbundle/ansible-playbook-bundle/issues/118
	oc run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- provision -e namespace=$(_TEST_PROJECT) || true
	oc run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- provision -e namespace=$(_TEST_PROJECT)
.PHONY: test-apb-provision

test-apb-deprovision: apb-push-to-local-broker
	oc run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- deprovision -e namespace=$(_TEST_PROJECT)
.PHONY: test-apb-deprovision
