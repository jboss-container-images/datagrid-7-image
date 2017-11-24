#!/bin/bash
# Add custom launch script and helpers
# NOTE: this overrides the openshift-launch.sh script in os-eap64-launch
set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

cp -rfp ${ADDED_DIR}/datagrid-online-services.sh $JBOSS_HOME/bin
