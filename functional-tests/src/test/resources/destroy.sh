#!/bin/bash

echo "---- Printing out test resources ----"
oc get all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts

echo "---- Describe Pods ----"
oc describe pods

echo "---- Docker PS ----"
docker ps

echo "---- Caching Service logs ----"
oc logs caching-service-app-0
oc logs caching-service-app-0 -c pem-to-keystore

echo "---- Shared Memory logs ----"
oc logs shared-memory-service-app-0
oc logs shared-memory-service-app-0 -c pem-to-keystore

echo "---- Test Runner logs ----"
oc logs testrunner
oc logs testrunner -c pem-to-truststore

echo "---- Clearing up test resources ---"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
oc delete template caching-service || true
oc delete template shared-memory-service || true

oc delete service caching-service-app-http-helper-project
oc delete service caching-service-app-hotrod-helper-project
oc delete service shared-memory-service-app-http-helper-project
oc delete service shared-memory-service-app-hotrod-helper-project

