#!/bin/bash

echo "---- Printing out test resources ----"
oc get all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts

echo "---- Clearing up test resources ---"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=shared-memory-service || true
oc delete template caching-service || true
oc delete template shared-memory-service || true
