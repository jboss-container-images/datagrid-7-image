#!/bin/sh

CONFIG_FILE=$JBOSS_HOME/standalone/configuration/cloud.xml
JGROUPS_STACK=kubernetes
LOGGING_FILE=$JBOSS_HOME/standalone/configuration/logging.properties

CONFIGURE_SCRIPTS=(
  $JBOSS_HOME/bin/launch/ha.sh
  /opt/run-java/proxy-options
)

source $JBOSS_HOME/bin/launch/configure.sh

$JBOSS_HOME/bin/launch/jdg-online-configuration.sh

echo "Running $JBOSS_IMAGE_NAME image, version $JBOSS_IMAGE_VERSION"

exec $JBOSS_HOME/bin/standalone.sh -c cloud.xml -bmanagement 127.0.0.1 ${JBOSS_HA_ARGS} ${JAVA_PROXY_OPTIONS} -Djboss.default.jgroups.stack=$JGROUPS_STACK --debug
