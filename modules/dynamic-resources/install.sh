#!/bin/sh

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

mkdir -p /usr/local/dynamic-resources
cp -p $ADDED_DIR/dynamic_resources.sh $JBOSS_HOME/bin/launch
