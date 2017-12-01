#!/bin/bash

echo "---- Printing out test resources ----"
oc get all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts

echo "---- Docker PS ----"
docker ps

echo "---- Caching Service logs ----"
oc logs caching-service-app-0

echo "---- Shared Memory logs ----"
oc logs shared-memory-service-app-0

echo "---- Clearing up test resources ---"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
oc delete template caching-service || true
oc delete template shared-memory-service || true
