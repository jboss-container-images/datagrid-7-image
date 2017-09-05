#!/bin/bash

# This name is hardcoded in Makefile. We need a fixed name to push it to local OpenShift registry
IMAGE_NAME=${image:-jboss-datagrid-services/caching-service}

echo "---- Clearing up (any potential) leftovers ----"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=jdg-caching-service || true
oc delete template jdg-caching-service || true

echo "---- Creating Caching Service for test ----"
echo "Current dir $PWD"
echo "Using image $IMAGE_NAME"

oc create -f ../templates/caching-service.json
oc process jdg-caching-service -p NAMESPACE=$(oc project -q) -p IMAGE=${IMAGE_NAME} | oc create -f -
