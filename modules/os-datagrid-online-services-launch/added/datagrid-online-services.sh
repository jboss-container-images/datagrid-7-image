#!/bin/sh

PROFILE=${PROFILE:=CACHING-SERVICE}
PROFILE=${PROFILE,,}
CONFIG_FILE=${CONFIG_FILE:=cloud.xml}
JGROUPS_STACK=kubernetes
LOGGING_FILE=$JBOSS_HOME/standalone/configuration/logging.properties

if [ "x$JAVA_OPTS" == "x" ]; then
   echo "No JAVA_OPTS variable provided, configuring heap settings automatically."
   source $JBOSS_HOME/bin/launch/dynamic_resources.sh > /dev/null 2>&1
   JAVA_OPTS="$(adjust_java_options)"
   echo "Inferred JAVA_OPTS=${JAVA_OPTS}"
fi

CONFIGURE_SCRIPTS=(
  $JBOSS_HOME/bin/launch/ha.sh
  /opt/run-java/proxy-options
)

source $JBOSS_HOME/bin/launch/configure.sh

$JBOSS_HOME/bin/launch/jdg-online-configuration.sh --profile=$PROFILE
if [[ $? -ne 0 ]]; then
    echo "WARNING: Profile ${PROFILE} doesn't exist. Falling back to Caching Service."
fi

echo "Running $JBOSS_IMAGE_NAME image, version $JBOSS_IMAGE_VERSION"

exec $JBOSS_HOME/bin/standalone.sh -c ${CONFIG_FILE} -bmanagement 127.0.0.1 ${JBOSS_HA_ARGS} ${JAVA_PROXY_OPTIONS} -Djboss.default.jgroups.stack=$JGROUPS_STACK $*
