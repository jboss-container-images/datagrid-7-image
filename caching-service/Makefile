start-openshift-with-catalog:
	oc cluster up --service-catalog
	oc login -u system:admin
	oc adm policy add-cluster-role-to-user cluster-admin developer
	oc login -u developer -p developer
	oc project openshift
	oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated
.PHONY: start-openshift-with-catalog

stop-openshift:
	oc cluster down
.PHONY: stop-openshift

build-image:
	condev generate -b
.PHONY: build-image

install-templates:
	oc create -f templates/caching-service.json || true
.PHONY: install-templates

clear-templates:
	oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=jdg-caching-service || true
	oc delete template jdg-caching-service || true
.PHONY: clear-templates

test-caching-service:
	oc process jdg-caching-service -p NAMESPACE=$(shell oc project -q) | oc create -f -
.PHONY: test-caching-service

test:
	mvn clean test -f modules/os-jdg-caching-service-configuration/pom.xml
	oc cluster up
	mvn clean test -f functional-tests/pom.xml || (ret=$$?; oc cluster down && exit $$ret)
.PHONY: test
