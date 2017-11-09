#!/bin/sh

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

cp -rfp ${ADDED_DIR}/adjust_performance_logging.sh $JBOSS_HOME/bin/launch
cp -rfp ${ADDED_DIR}/measure_performance.sh /usr/bin
chmod ug+x /usr/bin/measure_performance.sh

echo 'source $JBOSS_HOME/bin/launch/adjust_performance_logging.sh' >> $JBOSS_HOME/bin/standalone.conf
echo 'JAVA_OPTS=$(adjust_java_performance_logging ${JAVA_OPTS})' >> $JBOSS_HOME/bin/standalone.conf
