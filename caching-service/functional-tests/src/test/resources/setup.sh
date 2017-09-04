#!/bin/bash

echo "---- Clearing up (any potential) leftovers ----"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=jdg-caching-service || true
oc delete template jdg-caching-service || true

echo "---- Creating Caching Service for test ----"
echo "Current dir $PWD"

oc create -f ../templates/caching-service.json
oc process jdg-caching-service -p NAMESPACE=$(oc project -q) | oc create -f -
