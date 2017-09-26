DEV_IMAGE_NAME = datagrid-online-services-dev
DEV_APB_IMAGE_NAME = datagrid-online-services-apb-dev
DEV_IMAGE_ORG = jboss-dataservices
DEV_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME)
DEV_APB_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME)
MVN_COMMAND = mvn

# You may replace it with your custom command. See https://github.com/ansibleplaybookbundle/ansible-playbook-bundle#installing-the-apb-tool
APB_COMMAND = docker run --rm --privileged -v `pwd`:/mnt -v ${HOME}/.kube:/.kube -v /var/run/docker.sock:/var/run/docker.sock -u `id -u` docker.io/ansibleplaybookbundle/apb

_TEST_PROJECT = myproject
_REGISTRY_IP = $(shell oc get svc/docker-registry -n default -o yaml | grep 'clusterIP:' | awk '{print $$2}')
_ANSIBLE_SERVICE_BROKER_IP = $(shell oc get svc/asb -n ansible-service-broker -o yaml | grep 'clusterIP:' | awk '{print $$2}')
_IMAGE = $(_REGISTRY_IP):5000/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME)
_APB_IMAGE = $(_REGISTRY_IP):5000/$(_TEST_PROJECT)/$(DEV_APB_IMAGE_NAME)

start-openshift-with-catalog:
	@echo "---- Starting OpenShift ----"
	oc cluster up --service-catalog

	@echo "---- Granting admin rights to Developer ----"
	oc login -u system:admin
	oc adm policy add-cluster-role-to-user cluster-admin developer
	oc login -u developer -p developer

	@echo "---- Turning on Template Service Broker ----"
	oc project openshift
	oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated

	@echo "---- Installing Ansible Service Broker ----"
	oc new-project ansible-service-broker
	# The Ansible Service Broker page suggests to use  run_latest_build.sh script, but I hit
	# https://github.com/openshift/ansible-service-broker/issues/443
	# That's why we need a workaround.
	# The second thing is that sometimes a Route is not accessible (probably due to 443 issue I mentioned the above)
	# so we need to alter routing address to Service (not Route).
	( \
		cat service-broker/deploy-ansible-service-broker.template.yaml \
        | oc process \
        -n ansible-service-broker \
        -p BROKER_KIND="Broker" \
        -p INSECURE="true" \
        -p INSECURE_EDGE="Allow" \
        -p TERMINATION="edge" \
        -p ASB_SCHEME="http" \
        -p ENABLE_BASIC_AUTH="false" -f - | oc create -f - \
	)

	@echo "---- Switching to test project ----"
	oc project $(_TEST_PROJECT)
.PHONY: start-openshift-with-catalog

stop-openshift:
	oc cluster down
.PHONY: stop-openshift

build-image:
	( \
		virtualenv ~/concreate; \
		source ~/concreate/bin/activate; \
		pip install -U concreate==1.0.0rc2; \
		concreate generate --target target-docker; \
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
	oc adm policy add-role-to-user admin developer -n myproject || true
	oc adm policy add-role-to-user system:image-builder developer || true
.PHONY: _add_openshift_push_permissions

push-image-to-local-openshift: _add_openshift_push_permissions _login_to_openshift
	sudo docker tag $(DEV_IMAGE_FULL_NAME) $(_IMAGE)
	sudo docker push $(_IMAGE)
.PHONY: push-image-to-local-openshift

test-functional:
	$(MVN_COMMAND) clean test -f functional-tests/pom.xml -Dimage=$(_IMAGE)
.PHONY: test-functional

test-unit:
	$(MVN_COMMAND) clean test -f modules/os-datagrid-online-services-configuration/pom.xml
.PHONY: test-functional

install-templates-in-openshift-namespace:
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
	oc process caching-service -p NAMESPACE=$(shell oc project -q) | oc create -f -
.PHONY: test-caching-service-manually

test-shared-memory-service-manually:
	oc process shared-memory-service -p NAMESPACE=$(shell oc project -q) | oc create -f -
.PHONY: test-shared-memory-service-manually

clean-maven:
	$(MVN_COMMAND) clean -f modules/os-jdg-caching-service-configuration/pom.xml || true
	$(MVN_COMMAND) clean -f functional-tests/pom.xml || true
.PHONY: clean-maven

clean-docker:
	sudo docker rmi $(_IMAGE) || true
	sudo docker rmi $(_APB_IMAGE) || true
.PHONY: clean-docker

clean: clean-docker clean-maven stop-openshift
.PHONY: clean

test-ci: test-unit start-openshift-with-catalog build-image push-image-to-local-openshift test-functional clean
.PHONY: test-ci

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

apb-push-to-local-broker: _add_openshift_push_permissions _add_apb_roles apb-build _login_to_openshift
	(\
		cd service-broker/datagrid-online-services-apb; \
		$(APB_COMMAND) push --broker http://$(_ANSIBLE_SERVICE_BROKER_IP):1338; \
		$(APB_COMMAND) list --broker http://$(_ANSIBLE_SERVICE_BROKER_IP):1338; \
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
