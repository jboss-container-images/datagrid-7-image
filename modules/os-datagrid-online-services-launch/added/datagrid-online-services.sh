#!/bin/sh

function checkOptionalParamExists() {
   var=$1
   val=${!var}
   if [ -z ${val} ]; then
       echo "${var} param not set, falling back to '$2'" >&2
       echo $2
   else
       echo "$var=$val" >&2
   fi
   echo ${val}
}

PROFILE=${PROFILE,,}
PROFILE=$(checkOptionalParamExists "PROFILE" "caching-service")
CONFIG_FILE=$(checkOptionalParamExists "CONFIG_FILE" "services.xml")
JGROUPS_STACK=kubernetes
LOGGING_FILE=$JBOSS_HOME/standalone/configuration/logging.properties

CONFIGURE_SCRIPTS=(
  $JBOSS_HOME/bin/launch/ha.sh
  $JBOSS_HOME/bin/launch/adjust_memory.sh
  /opt/run-java/proxy-options
)

source $JBOSS_HOME/bin/launch/configure.sh

$JBOSS_HOME/bin/launch/jdg-online-configuration.sh --profile $PROFILE "eviction_total_memory_bytes=$EVICTION_TOTAL_MEMORY_B" \
"num_owners=${NUMBER_OF_OWNERS}" "keystore_file=${KEYSTORE_FILE}" "keystore_password=${KEYSTORE_PASSWORD}"

if [[ $? -ne 0 ]]; then
    echo "ERROR: Service configuration failed, TERMINATING."
    exit 1
fi

echo "Running $JBOSS_IMAGE_NAME image, version $JBOSS_IMAGE_VERSION"

exec $JBOSS_HOME/bin/standalone.sh -c ${CONFIG_FILE} -bmanagement 127.0.0.1 ${JBOSS_HA_ARGS} ${JAVA_PROXY_OPTIONS} -Djboss.default.jgroups.stack=$JGROUPS_STACK $*
