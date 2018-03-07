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
IMAGE_FULL_NAME = $(DOCKER_REGISTRY_ENGINEERING)/$(DEV_IMAGE_ORG)/$(IMAGE_NAME)
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

DOCKER_MEMORY=512M

MVN_COMMAND = mvn

# You may replace it with your custom command. See https://github.com/ansibleplaybookbundle/ansible-playbook-bundle#installing-the-apb-tool
APB_COMMAND = docker run --rm --privileged -v `pwd`:/mnt -v ${HOME}/.kube:/.kube -v /var/run/docker.sock:/var/run/docker.sock -u `id -u` docker.io/ansibleplaybookbundle/apb

_TEST_PROJECT = myproject

#Set variables for remote openshift when OPENSHIFT_ONLINE_REGISTRY is defined
ifeq ($(OPENSHIFT_ONLINE_REGISTRY),)
_OPENSHIFT_MASTER = https://127.0.0.1:8443
_DOCKER_REGISTRY = "$(shell oc get svc/docker-registry -n default -o yaml | grep 'clusterIP:' | awk '{print $$2}'):5000"
_IMAGE = $(_DOCKER_REGISTRY)/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME)
_APB_IMAGE = $(_DOCKER_REGISTRY):5000/$(_TEST_PROJECT)/$(DEV_APB_IMAGE_NAME)
_OPENSHIFT_USERNAME = developer
_OPENSHIFT_PASSWORD = developer
_TESTRUNNER_PORT = 80
else
_DOCKER_REGISTRY = $(OPENSHIFT_ONLINE_REGISTRY)
_IMAGE = $(_DOCKER_REGISTRY)/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME)
_TESTRUNNER_PORT = 80
endif

_DEV_IMAGE_STREAM = $(DEV_IMAGE_NAME):latest

# This username and password is hardcoded (and base64 encoded) in the Ansible
# Service Broker template
_ANSIBLE_SERVICE_BROKER_USERNAME = admin
_ANSIBLE_SERVICE_BROKER_PASSWORD = admin

start-openshift-with-catalog:
	@echo "---- Starting OpenShift ----"
	oc cluster up --service-catalog
	@echo "---- Granting admin rights to Developer ----"
	oc login -u system:admin
	oc adm policy add-cluster-role-to-user cluster-admin $(_OPENSHIFT_USERNAME)

	@echo "---- Allowing containers to run specific users ----"
	# Some of the JDK commands (jcmd, jps etc.) require the same user as the one running java process.
	# The command below enabled that. The process inside the container will be ran using jboss user.
	# The same users will be used by default for `oc rsh` command.
	oc adm policy add-scc-to-group anyuid system:authenticated
.PHONY: start-openshift-with-catalog

prepare-openshift-project: clean-openshift
	@echo "---- Create main project for test purposes"
	oc new-project $(_TEST_PROJECT)

	@echo "---- Switching to test project ----"
	oc project $(_TEST_PROJECT)
.PHONY: prepare-openshift-project

clean-openshift:
	@echo "---- Deleting projects ----"
	oc delete project $(_TEST_PROJECT) || true
	( \
		while oc get projects | grep -e $(_TEST_PROJECT) > /dev/null; do \
			echo "Waiting for deleted projects..."; \
			sleep 5; \
		done; \
	)
.PHONY: clean-openshift

login-to-openshift:
	@echo "---- Login ----"
	oc login $(_OPENSHIFT_MASTER) -u $(_OPENSHIFT_USERNAME) -p $(_OPENSHIFT_PASSWORD)
.PHONY: login-to-openshift

start-openshift-with-catalog-and-ansible-service-broker: start-openshift-with-catalog login-to-openshift prepare-openshift-project install-ansible-service-broker
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
		pip install -U concreate==1.4.1; \
		$(CONCREATE_CMD) \
		deactivate; \
	)
	sudo docker build --force-rm -t $(DEV_IMAGE_FULL_NAME) ./target-docker/image
.PHONY: build-image

_login_to_docker:
	sudo docker login -u $(shell oc whoami) -p $(shell oc whoami -t) $(_DOCKER_REGISTRY)
.PHONY: _login_to_docker

_wait_for_local_docker_registry:
	( \
		until oc get pod -n default | grep docker-registry | grep "1/1" > /dev/null; do \
			sleep 10; \
			echo "Waiting for Docker Registry..."; \
		done; \
	)
.PHONY: _wait_for_local_docker_registry

_add_openshift_push_permissions:
	oc adm policy add-role-to-user system:registry $(_OPENSHIFT_USERNAME) || true
	oc adm policy add-role-to-user admin $(_OPENSHIFT_USERNAME) -n ${_TEST_PROJECT} || true
	oc adm policy add-role-to-user system:image-builder $(_OPENSHIFT_USERNAME) || true
.PHONY: _add_openshift_push_permissions

push-image-to-local-openshift: _add_openshift_push_permissions _wait_for_local_docker_registry _login_to_docker push-image-common
.PHONY: push-image-to-local-openshift

push-image-to-online-openshift: _login_to_docker push-image-common
.PHONY: push-image-to-online-openshift

push-image-common:
	sudo docker tag $(DEV_IMAGE_FULL_NAME) $(_IMAGE)
	sudo docker push $(_IMAGE)
	oc set image-lookup $(DEV_IMAGE_NAME)
.PHONY: push-image-common

test-functional: deploy-testrunner-route
	$(MVN_COMMAND) -Dimage=$(_DEV_IMAGE_STREAM) -Dkubernetes.auth.token=$(shell oc whoami -t) -DDOCKER_REGISTRY_REDHAT=$(DOCKER_REGISTRY_REDHAT) -DTESTRUNNER_HOST=$(shell oc get routes | grep testrunner | awk '{print $$2}') -DTESTRUNNER_PORT=${_TESTRUNNER_PORT} clean test -f functional-tests/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-functional

deploy-testrunner-route:
	oc create -f ./functional-tests/src/test/resources/eap7-testrunner-service.json
	oc create -f ./functional-tests/src/test/resources/eap7-testrunner-route.json
.PHONY: deploy-testrunner-route

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
	oc set image-lookup $(DEV_IMAGE_NAME)
	oc process caching-service -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_DEV_IMAGE_STREAM) | oc create -f -
	oc expose svc/caching-service-app-http || true
	oc expose svc/caching-service-app-hotrod || true
	oc get routes
.PHONY: test-caching-service-manually

test-shared-memory-service-manually:
	oc set image-lookup $(DEV_IMAGE_NAME)
	oc process shared-memory-service -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_DEV_IMAGE_STREAM) | oc create -f -
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

test-ci: clean test-unit start-openshift-with-catalog login-to-openshift prepare-openshift-project build-image push-image-to-local-openshift test-functional
.PHONY: test-ci

#Before running this target, login to the remote OpenShift from console in whatever way recommended by the provider
test-remote: clean-docker clean-maven prepare-openshift-project build-image push-image-to-online-openshift test-functional
.PHONY: test-online

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

run-docker: build-image
	$(shell mkdir -p ./capacity-tests/target/heapdumps)
	$(shell chmod 777 ./capacity-tests/target/heapdumps)
	# For some tests it is a good idea to add --memory-swappiness=0 --memory-swap $(DOCKER_MEMORY)
	# This prevents container from swapping but on the other hand, you won't be able
	# to allocate additional memory needed for the heap dump!
	docker run --privileged=true -m $(DOCKER_MEMORY) -e APPLICATION_USER=test -e APPLICATION_USER_PASSWORD=test -e KEYSTORE_FILE=/tmp/keystores/keystore_server.jks -e DEBUG=true -e KEYSTORE_PASSWORD=secret -v $(shell pwd)/capacity-tests/src/test/resources:/tmp/keystores -v $(shell pwd)/capacity-tests/target/heapdumps:/tmp/heapdumps $(ADDITIONAL_ARGUMENTS) $(DEV_IMAGE_FULL_NAME)
.PHONY: run-docker

test-capacity:
	$(MVN_COMMAND) clean test -f capacity-tests/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-capacity

run-caching-service-locally: stop-openshift start-openshift-with-catalog login-to-openshift prepare-openshift-project build-image push-image-to-local-openshift install-templates test-caching-service-manually
.PHONY: run-caching-service-locally

run-shared-memory-service-locally: stop-openshift start-openshift-with-catalog login-to-openshift prepare-openshift-project build-image push-image-to-local-openshift install-templates test-shared-memory-service-manually
.PHONY: run-caching-service-locally

#Before running this target, login to the remote OpenShift from console in whatever way recommended by the provider, make sure you specify the _TEST_PROJECT and OPENSHIFT_ONLINE_REGISTRY variables
run-caching-service-remotely: clean-docker clean-maven prepare-openshift-project build-image push-image-to-online-openshift install-templates test-caching-service-manually
.PHONY: test-online

#Before running this target, login to the remote OpenShift from console in whatever way recommended by the provider, make sure you specify the _TEST_PROJECT and OPENSHIFT_ONLINE_REGISTRY variables
run-shared-memory-service-remotely: clean-docker clean-maven prepare-openshift-project build-image push-image-to-online-openshift install-templates test-shared-memory-service-manually
.PHONY: test-online
