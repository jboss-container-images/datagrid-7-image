# This is a simplified version of `ha.sh` script from CCT modules.
# We do not need to mess with JGroups here, since we render configuration differently.

source ${JBOSS_HOME}/bin/launch/openshift-node-name.sh

function prepareEnv() {
  unset NODE_NAME
}

function configure() {
  configure_ha
}

function configure_ha() {
  IP_ADDR=`hostname -i`
  JBOSS_HA_ARGS="-b ${IP_ADDR} -bprivate ${IP_ADDR}"
  init_node_name
}
