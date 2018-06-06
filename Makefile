DEV_IMAGE_ORG = datagrid-7
DEV_IMAGE_NAME = datagrid-services-dev
DEV_IMAGE_TAG = latest
DOCKER_REGISTRY_ENGINEERING =
DOCKER_REGISTRY_REDHAT =
ADDITIONAL_ARGUMENTS =
INSTALL_OC =

CE_DOCKER = $(shell docker version | grep Version | head -n 1 | grep -e "-ce")
ifneq ($(CE_DOCKER),)
DOCKER_REGISTRY_ENGINEERING = docker-registry.engineering.redhat.com
DOCKER_REGISTRY_REDHAT = registry.access.redhat.com/
DEV_IMAGE_FULL_NAME = $(DOCKER_REGISTRY_ENGINEERING)/$(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME):$(DEV_IMAGE_TAG)
IMAGE_FULL_NAME = $(DOCKER_REGISTRY_ENGINEERING)/$(DEV_IMAGE_ORG)/$(IMAGE_NAME):$(DEV_IMAGE_TAG)
CONCREATE_CMD = concreate build --overrides=overrides.yaml --target target-docker --tag $(DEV_IMAGE_FULL_NAME)
else
DEV_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME):$(DEV_IMAGE_TAG)
CONCREATE_CMD = concreate build --target target-docker --tag $(DEV_IMAGE_FULL_NAME)
endif

# In order to test this image we need to do a little trick. The APB image is pushed under the following name:
# http://$REGISTRY:5000/myproject/datagrid-services-apb
# Since the project name (myproject) and image name (datagrid-services-apb) match
# OpenShift "thinks" that this image has already been pulled from some registry.
# But the reality is different - we pushed it...
DEV_APB_IMAGE_NAME = datagrid-services-apb
DEV_APB_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_APB_IMAGE_NAME)

DOCKER_MEMORY=512M

MVN_COMMAND = mvn
OC_COMMAND = ./oc

# You may replace it with your custom command. See https://github.com/ansibleplaybookbundle/ansible-playbook-bundle#installing-the-apb-tool
APB_COMMAND = docker run --rm --privileged -v `pwd`:/mnt -v ${HOME}/.kube:/.kube -v /var/run/docker.sock:/var/run/docker.sock -u `id -u` docker.io/ansibleplaybookbundle/apb

_TEST_PROJECT = myproject

#Set variables for remote openshift when OPENSHIFT_ONLINE_REGISTRY is defined
ifeq ($(OPENSHIFT_ONLINE_REGISTRY),)
_OPENSHIFT_MASTER = https://127.0.0.1:8443
_DOCKER_REGISTRY = "$(shell $(OC_COMMAND) get svc/docker-registry -n default -o yaml | grep 'clusterIP:' | awk '{print $$2}'):5000"
_IMAGE = $(_DOCKER_REGISTRY)/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME):$(DEV_IMAGE_TAG)
_APB_IMAGE = $(_DOCKER_REGISTRY):5000/$(_TEST_PROJECT)/$(DEV_APB_IMAGE_NAME)
_OPENSHIFT_USERNAME = developer
_OPENSHIFT_PASSWORD = developer
_TESTRUNNER_PORT = 80
else
_DOCKER_REGISTRY = $(OPENSHIFT_ONLINE_REGISTRY)
_IMAGE = $(_DOCKER_REGISTRY)/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME):$(DEV_IMAGE_TAG)
_TESTRUNNER_PORT = 80
endif

_DEV_IMAGE_STREAM = $(DEV_IMAGE_NAME):$(DEV_IMAGE_TAG)

# This username and password is hardcoded (and base64 encoded) in the Ansible
# Service Broker template
_ANSIBLE_SERVICE_BROKER_USERNAME = admin
_ANSIBLE_SERVICE_BROKER_PASSWORD = admin

# This target provides a workaround for https://github.com/openshift/origin/issues/19663
# If the OpenShift Team decided to bring the old behavior back (ImageStreams to work with StatefulSets
# out of the box), just delete it.
_install-oc:
ifneq ("$(wildcard ./oc)","")
	echo "---- The oc binary has been previously installed. Skipping. ----"
else
ifeq ($(INSTALL_OC),)
	echo "---- Using OS installed oc client ----"
	ln -f -s `which oc` oc
else
	echo "---- Downloading and installing oc client ----"
	wget -N -L https://github.com/openshift/origin/releases/download/v3.7.2/openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit.tar.gz
	tar -zxf openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit.tar.gz
	mv openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit/$(OC_COMMAND) ./oc
	rm -rf openshift-origin-client-tools-v3.7.2-282e43f-linux-64bit
endif
endif
.PHONY: _install-oc

start-openshift-with-catalog: _install-oc
	@echo "---- Starting OpenShift ----"
	$(OC_COMMAND) cluster up --service-catalog
	@echo "---- Granting admin rights to Developer ----"
	$(OC_COMMAND) login -u system:admin
	$(OC_COMMAND) adm policy add-cluster-role-to-user cluster-admin $(_OPENSHIFT_USERNAME)

	@echo "---- Allowing containers to run specific users ----"
	# Some of the JDK commands (jcmd, jps etc.) require the same user as the one running java process.
	# The command below enabled that. The process inside the container will be ran using jboss user.
	# The same users will be used by default for `$(OC_COMMAND) rsh` command.
	$(OC_COMMAND) adm policy add-scc-to-group anyuid system:authenticated
.PHONY: start-openshift-with-catalog

prepare-openshift-project: clean-openshift
	@echo "---- Create main project for test purposes"
	$(OC_COMMAND) new-project $(_TEST_PROJECT)

	@echo "---- Switching to test project ----"
	$(OC_COMMAND) project $(_TEST_PROJECT)
.PHONY: prepare-openshift-project

clean-openshift: _install-oc
	@echo "---- Deleting projects ----"
	$(OC_COMMAND) delete project $(_TEST_PROJECT) || true
	( \
		while $(OC_COMMAND) get projects | grep -e $(_TEST_PROJECT) > /dev/null; do \
			echo "Waiting for deleted projects..."; \
			sleep 5; \
		done; \
	)
.PHONY: clean-openshift

login-to-openshift: _install-oc
	@echo "---- Login ----"
	$(OC_COMMAND) login $(_OPENSHIFT_MASTER) -u $(_OPENSHIFT_USERNAME) -p $(_OPENSHIFT_PASSWORD)
.PHONY: login-to-openshift

start-openshift-with-catalog-and-ansible-service-broker: start-openshift-with-catalog login-to-openshift prepare-openshift-project install-ansible-service-broker
.PHONY: start-openshift-with-catalog-and-ansible-service-broker

install-ansible-service-broker:
	@echo "---- Installing Ansible Service Broker ----"
	$(OC_COMMAND) new-project ansible-service-broker
	( \
		curl -s https://raw.githubusercontent.com/openshift/ansible-service-broker/master/templates/deploy-ansible-service-broker.template.yaml \
        | $(OC_COMMAND) process \
        -n ansible-service-broker \
        -p BROKER_KIND="Broker" \
        -p BROKER_AUTH="{\"basicAuthSecret\":{\"namespace\":\"ansible-service-broker\",\"name\":\"asb-auth-secret\"}}" \
        -p ENABLE_BASIC_AUTH="true" -f - | $(OC_COMMAND) create -f - \
	)
.PHONY: install-ansible-service-broker

stop-openshift: _install-oc
	$(OC_COMMAND) cluster down
.PHONY: stop-openshift

build-image:
	$(CONCREATE_CMD)
	concreate build --target target-docker --tag $(DEV_IMAGE_FULL_NAME)
.PHONY: build-image

_login_to_docker:
	sudo docker login -u $(shell $(OC_COMMAND) whoami) -p $(shell $(OC_COMMAND) whoami -t) $(_DOCKER_REGISTRY)
.PHONY: _login_to_docker

_wait_for_local_docker_registry:
	( \
		until $(OC_COMMAND) get pod -n default | grep docker-registry | grep "1/1" > /dev/null; do \
			sleep 10; \
			echo "Waiting for Docker Registry..."; \
		done; \
	)
.PHONY: _wait_for_local_docker_registry

_add_openshift_push_permissions:
	$(OC_COMMAND) adm policy add-role-to-user system:registry $(_OPENSHIFT_USERNAME) || true
	$(OC_COMMAND) adm policy add-role-to-user admin $(_OPENSHIFT_USERNAME) -n ${_TEST_PROJECT} || true
	$(OC_COMMAND) adm policy add-role-to-user system:image-builder $(_OPENSHIFT_USERNAME) || true
.PHONY: _add_openshift_push_permissions

push-image-to-local-openshift: _add_openshift_push_permissions _wait_for_local_docker_registry _login_to_docker push-image-common
.PHONY: push-image-to-local-openshift

push-image-to-online-openshift: _login_to_docker push-image-common
.PHONY: push-image-to-online-openshift

pull-image:
	docker pull $(DEV_IMAGE_FULL_NAME)
.PHONY: pull-image

push-image-common:
	sudo docker tag $(DEV_IMAGE_FULL_NAME) $(_IMAGE)
	sudo docker push $(_IMAGE)
	$(OC_COMMAND) set image-lookup $(DEV_IMAGE_NAME)
.PHONY: push-image-common

test-functional: deploy-testrunner-route
	$(MVN_COMMAND) -Dimage=$(_DEV_IMAGE_STREAM) -Dkubernetes.auth.token=$(shell $(OC_COMMAND) whoami -t) -DDOCKER_REGISTRY_REDHAT=$(DOCKER_REGISTRY_REDHAT) -DTESTRUNNER_HOST=$(shell $(OC_COMMAND) get routes | grep testrunner | awk '{print $$2}') -DTESTRUNNER_PORT=${_TESTRUNNER_PORT} clean test -f functional-tests/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-functional

deploy-testrunner-route:
	$(OC_COMMAND) create -f ./functional-tests/src/test/resources/eap7-testrunner-service.json
	$(OC_COMMAND) create -f ./functional-tests/src/test/resources/eap7-testrunner-route.json
.PHONY: deploy-testrunner-route

test-unit:
	$(MVN_COMMAND) clean test -f modules/os-datagrid-online-services-configuration/pom.xml $(ADDITIONAL_ARGUMENTS)
.PHONY: test-unit

_relist-template-service-broker:
	# This one is very hacky - the idea is to increase the relist request counter by 1. This way we ask the Template
	# Service Broker to refresh all templates. The rest of the complication is due to how Makefile parses file.
	RELIST_TO_BE_SET=`expr $(shell $(OC_COMMAND) get ClusterServiceBroker/template-service-broker --template={{.spec.relistRequests}}) + 1` && \
	$(OC_COMMAND) patch ClusterServiceBroker/template-service-broker -p '{"spec":{"relistRequests": '$$RELIST_TO_BE_SET'}}'
.PHONY: _relist-template-service-broker

_install_templates_in_openshift_namespace:
	$(OC_COMMAND) create -f templates/caching-service.json -n openshift || true
	$(OC_COMMAND) create -f templates/shared-memory-service.json -n openshift || true
.PHONY: _install_templates_in_openshift_namespace

install-templates-in-openshift-namespace: _install_templates_in_openshift_namespace _relist-template-service-broker
.PHONY: install-templates-in-openshift-namespace

install-templates:
	$(OC_COMMAND) create -f templates/caching-service.json || true
	$(OC_COMMAND) create -f templates/shared-memory-service.json || true
.PHONY: install-templates

clear-templates:
	$(OC_COMMAND) delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
	$(OC_COMMAND) delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
	$(OC_COMMAND) delete template caching-service || true
	$(OC_COMMAND) delete template shared-memory-service || true
.PHONY: clear-templates

test-caching-service-manually:
	$(OC_COMMAND) set image-lookup $(DEV_IMAGE_NAME)
	$(OC_COMMAND) process caching-service -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_DEV_IMAGE_STREAM) | $(OC_COMMAND) create -f -
	$(OC_COMMAND) expose svc/caching-service-app-http || true
	$(OC_COMMAND) expose svc/caching-service-app-hotrod || true
	$(OC_COMMAND) get routes
.PHONY: test-caching-service-manually

test-shared-memory-service-manually:
	$(OC_COMMAND) set image-lookup $(DEV_IMAGE_NAME)
	$(OC_COMMAND) process shared-memory-service -p APPLICATION_USER=test \
	-p APPLICATION_USER_PASSWORD=test -p IMAGE=$(_DEV_IMAGE_STREAM) | $(OC_COMMAND) create -f -
	$(OC_COMMAND) expose svc/shared-memory-service-app-http || true
	$(OC_COMMAND) expose svc/shared-memory-service-app-hotrod || true
	$(OC_COMMAND) get routes
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
.PHONY: test-remote

test-remote-with-pull: clean-docker clean-maven prepare-openshift-project pull-image push-image-to-online-openshift test-functional
.PHONY: test-remote-with-pull

clean-ci: clean-docker stop-openshift #avoid cleaning Maven as we need results to be reported by the job
.PHONY: clean-ci

apb-build:
	(\
		cd service-broker/datagrid-services-apb; \
		$(APB_COMMAND) prepare; \
		sudo docker build --force-rm -t $(DEV_APB_IMAGE_FULL_NAME) ./; \
	)
.PHONY: apb-build

_add_apb_roles:
	$(OC_COMMAND) policy add-role-to-user cluster-admin system:serviceaccount:myproject:default -n myproject || true
.PHONY: _add_apb_roles

_wait_for_ansible_service_broker:
	( \
		until $(OC_COMMAND) get pods -n ansible-service-broker | grep asb | grep "2/2" > /dev/null; do \
			sleep 20; \
			echo "Waiting for Ansible Service Broker..."; \
		done; \
	)
.PHONY: _wait_for_ansible_service_broker

apb-push-to-local-broker: _add_openshift_push_permissions _add_apb_roles apb-build _login_to_openshift _wait_for_ansible_service_broker
	(\
		cd service-broker/datagrid-services-apb; \
		$(APB_COMMAND) push -u $(_ANSIBLE_SERVICE_BROKER_USERNAME) -p $(_ANSIBLE_SERVICE_BROKER_PASSWORD); \
		$(APB_COMMAND) list -u $(_ANSIBLE_SERVICE_BROKER_USERNAME) -p $(_ANSIBLE_SERVICE_BROKER_PASSWORD); \
	)
	sudo docker tag $(DEV_APB_IMAGE_FULL_NAME) $(_APB_IMAGE)
	sudo docker push $(_APB_IMAGE)
.PHONY: apb-push-to-local-broker

test-apb-provision: apb-push-to-local-broker
	# This needs to be called twice :(
	# https://github.com/ansibleplaybookbundle/ansible-playbook-bundle/issues/118
	$(OC_COMMAND) run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- provision -e namespace=$(_TEST_PROJECT) || true
	$(OC_COMMAND) run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- provision -e namespace=$(_TEST_PROJECT)
.PHONY: test-apb-provision

test-apb-deprovision: apb-push-to-local-broker
	$(OC_COMMAND) run apb-test --rm=true --image=$(DEV_APB_IMAGE_FULL_NAME) --restart=Never --attach=true -- deprovision -e namespace=$(_TEST_PROJECT)
.PHONY: test-apb-deprovision

run-docker: build-image
	$(shell mkdir -p ./capacity-tests/target/heapdumps)
	$(shell chmod 777 ./capacity-tests/target/heapdumps)
	docker run --privileged=true -m $(DOCKER_MEMORY) --memory-swappiness=0 --memory-swap $(DOCKER_MEMORY) -e APPLICATION_USER=test -e APPLICATION_USER_PASSWORD=test -e KEYSTORE_FILE=/tmp/keystores/keystore_server.jks -e DEBUG=true -e KEYSTORE_PASSWORD=secret -v $(shell pwd)/capacity-tests/src/test/resources:/tmp/keystores -v $(shell pwd)/capacity-tests/target/heapdumps:/tmp/heapdumps $(ADDITIONAL_ARGUMENTS) $(DEV_IMAGE_FULL_NAME)
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
