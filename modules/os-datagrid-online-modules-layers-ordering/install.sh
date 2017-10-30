#!/bin/bash
set -e

# Since KUBE_PING is now provided by the server distribution, we need to switch layer ordering
# (in other words, favor those jars that are provided by our modules (not CE).
# See https://issues.jboss.org/browse/ISPN-8452 for more details.
echo "layers=base,openshift" > /opt/datagrid/modules/layers.conf
