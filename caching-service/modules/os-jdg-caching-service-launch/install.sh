#!/bin/bash
# Add custom launch script and helpers
# NOTE: this overrides the openshift-launch.sh script in os-eap64-launch
set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

cp -rfp ${ADDED_DIR}/jdg-caching-service.sh $JBOSS_HOME/bin

# Add authentication config script
cp -rfp ${ADDED_DIR}/launch/authentication-config.sh $JBOSS_HOME/bin/launch
