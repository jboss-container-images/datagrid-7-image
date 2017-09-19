#!/bin/bash
set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

cp -rfp ${ADDED_DIR}/livenessProbe.sh $JBOSS_HOME/bin
cp -rfp ${ADDED_DIR}/readinessProbe.sh $JBOSS_HOME/bin
