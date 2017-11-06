#!/bin/bash

echo "params $*"

PROFILE="caching-service"
JBOSS_HOME="/opt/datagrid"
PROFILES_DIRECTORY="/opt/profiles"

function usage() {
    echo "Usage will be finished upon first release."
}

function check_if_profile_exists() {
   if [ ! -f "${PROFILES_DIRECTORY}/${PROFILE}.cli" ]; then
      echo "Profile $PROFILE doesn't exist."
      exit 1
   fi
}

function check_if_cli_exists() {
   if [ ! -f "${JBOSS_HOME}/bin/cli.sh" ]; then
      echo "CLI ${JBOSS_HOME}/bin/cli.sh doesn't exist."
      exit 1
   fi
}

function flush_additional_properties_to_file() {
   echo "" > "$JBOSS_HOME/bin/.jbossclirc"
   if [[ ! -z "$ADDITIONAL_PARAMETERS" ]]; then
      for i in "${ADDITIONAL_PARAMETERS[@]}"
      do
         echo "set $i" >> "$JBOSS_HOME/bin/.jbossclirc"
      done
      echo "CLI properties file: $(cat $JBOSS_HOME/bin/.jbossclirc)"
   fi
}

ADDITIONAL_PARAMETERS=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
   -h | --help)
   usage
   exit
   ;;
   --profile)
   PROFILE="$2"
   shift 2
   ;;
   --profiles-directory)
   PROFILES_DIRECTORY="$2"
   shift 2
   ;;
   --jboss-home)
   JBOSS_HOME="$2"
   shift 2
   ;;
   *)
   ADDITIONAL_PARAMETERS+=("$1")
   shift
   ;;
esac
done
set -- "${ADDITIONAL_PARAMETERS[@]}"

check_if_profile_exists
check_if_cli_exists
flush_additional_properties_to_file

$JBOSS_HOME/bin/cli.sh --file="${PROFILES_DIRECTORY}/${PROFILE}.cli"

exit $?
