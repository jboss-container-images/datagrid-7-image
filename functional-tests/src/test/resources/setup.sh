#!/bin/bash

# This name is hardcoded in Makefile. We need a fixed name to push it to local OpenShift registry
IMAGE_NAME=${image:-jboss-dataservices/datagrid-online-services}

echo "---- Clearing up (any potential) leftovers ----"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
oc delete template caching-service || true
oc delete template shared-memory-service || true

echo "---- Creating Caching Service for test ----"
echo "Current dir $PWD"
echo "Using image $IMAGE_NAME"

oc create -f ../templates/caching-service.json
oc create -f ../templates/shared-memory-service.json

oc process caching-service -p NAMESPACE=$(oc project -q) -p IMAGE=${IMAGE_NAME} -p APPLICATION_USER=test \
-p APPLICATION_USER_PASSWORD=test -p KEYSTORE_PASSWORD=test99 | oc create -f -

oc process shared-memory-service -p NAMESPACE=$(oc project -q) -p IMAGE=${IMAGE_NAME} \
-p APPLICATION_USER=test -p APPLICATION_USER_PASSWORD=test -p KEYSTORE_PASSWORD=test99 | oc create -f -
