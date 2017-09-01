#!/bin/bash
set -e

SCRIPT_DIR=$(dirname $0)
SCRIPT_DIR=${SCRIPT_DIR}/src/main/bash

cp -p ${SCRIPT_DIR}/jdg-online-configuration.sh $JBOSS_HOME/bin/launch

mkdir -p /opt/profiles
cp -r ${SCRIPT_DIR}/profiles/* /opt/profiles
