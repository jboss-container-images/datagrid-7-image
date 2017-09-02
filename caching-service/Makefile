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

push-image-to-local-openshift:
	docker tag slaskawi/caching-service:1.0 docker-registry-default.127.0.0.1.nip.io/slaskawi/caching-service:1.0
	docker push docker-registry-default.127.0.0.1.nip.io/slaskawi/caching-service:1.0
.PHONY: push-image-to-local-openshift

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
