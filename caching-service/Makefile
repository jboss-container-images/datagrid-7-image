DEV_IMAGE_NAME = caching-service-dev
DEV_IMAGE_ORG = jboss-datagrid-services
DEV_IMAGE_FULL_NAME = $(DEV_IMAGE_ORG)/$(DEV_IMAGE_NAME)

_TEST_PROJECT = myproject
_REGISTRY_IP = $(shell oc get svc/docker-registry -n default -o yaml | grep 'clusterIP:' | awk '{print $$2}')
_IMAGE = $(_REGISTRY_IP):5000/$(_TEST_PROJECT)/$(DEV_IMAGE_NAME)
_TEST_NAMESPACE = default

start-openshift-with-catalog:
	oc cluster up --service-catalog
	oc login -u system:admin
	oc adm policy add-cluster-role-to-user cluster-admin developer
	oc login -u developer -p developer
	oc project openshift
	oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated
	oc project $(_TEST_PROJECT)
.PHONY: start-openshift-with-catalog

stop-openshift:
	oc cluster down
.PHONY: stop-openshift

build-image:
	concreate image.yaml target
	sudo docker build --force-rm -t $(DEV_IMAGE_FULL_NAME) ./target/image
.PHONY: build-image

push-image-to-local-openshift:
	oc adm policy add-role-to-user system:registry developer
	oc adm policy add-role-to-user admin developer -n myproject
	oc adm policy add-role-to-user system:image-builder developer

	sudo docker login -u $(shell oc whoami) -p $(shell oc whoami -t) $(_REGISTRY_IP):5000
	sudo docker tag $(DEV_IMAGE_FULL_NAME) $(_IMAGE)
	sudo docker push $(_IMAGE)
.PHONY: push-image-to-local-openshift

test-functional:
	mvn clean test -f functional-tests/pom.xml -Dimage=$(_IMAGE)
.PHONY: test-functional

test-unit:
	mvn clean test -f modules/os-jdg-caching-service-configuration/pom.xml
.PHONY: test-functional

install-templates:
	oc create -f templates/caching-service.json || true
.PHONY: install-templates

clear-templates:
	oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=jdg-caching-service || true
	oc delete template jdg-caching-service || true
.PHONY: clear-templates

test-caching-service-manually:
	oc process jdg-caching-service -p NAMESPACE=$(shell oc project -q) | oc create -f -
.PHONY: test-caching-service-manually

clean-maven:
	mvn clean -f modules/os-jdg-caching-service-configuration/pom.xml || true
	mvn clean -f functional-tests/pom.xml || true
.PHONY: clean-maven

clean-docker:
	sudo docker rmi $(_IMAGE) || true
.PHONY: clean-docker

clean: clean-docker clean-maven stop-openshift
.PHONY: clean

test-ci: start-openshift-with-catalog build-image test-unit push-image-to-local-openshift test-functional clean
.PHONY: test-ci

