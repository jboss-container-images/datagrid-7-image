#!/bin/sh

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

cp -rfp ${ADDED_DIR}/standalone.conf $JBOSS_HOME/bin
cp -rfp ${ADDED_DIR}/adjust_memory.sh $JBOSS_HOME/bin/launch
