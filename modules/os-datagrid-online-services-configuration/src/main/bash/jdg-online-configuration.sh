#!/bin/bash

PROFILE="caching-service"
JBOSS_HOME="/opt/datagrid"
PROFILES_DIRECTORY="/opt/profiles"
DEBUG=${DEBUG:-false}

if [ "${DEBUG}" == "true" ]; then
   echo "params $*"
fi

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
   if [ "${DEBUG}" == "true" ]; then
      echo "set std_out=echo" >> "$JBOSS_HOME/bin/.jbossclirc"
   else
      echo "set std_out=discard" >> "$JBOSS_HOME/bin/.jbossclirc"
   fi
   if [[ ! -z "$ADDITIONAL_PARAMETERS" ]]; then
      for i in "${ADDITIONAL_PARAMETERS[@]}"
      do
         echo "set $i" >> "$JBOSS_HOME/bin/.jbossclirc"
      done
      if [ "${DEBUG}" == "true" ]; then
         echo "CLI properties file: $(cat $JBOSS_HOME/bin/.jbossclirc)"
      fi
   fi
}

function add_user() {
   role=${1}
   user="${role}_USER"
   pass="${role}_USER_PASSWORD"
   if [ -z ${!user} ]; then
       echo "${user} param must be set"
       exit 1
   fi

   if [ -z ${!pass} ]; then
       echo "${pass} param must be set"
       exit 1
   fi

   realm=$([ "${role}" == "APPLICATION" ] && echo "-a")

   if [ "${DEBUG}" == "true" ]; then
      $JBOSS_HOME/bin/add-user.sh ${realm} -u ${!user} -p ${!pass}
   else
      $JBOSS_HOME/bin/add-user.sh ${realm} -u ${!user} -p ${!pass} >/dev/null 2>&1
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
add_user "APPLICATION"

if [ "${DEBUG}" == "true" ]; then
   $JBOSS_HOME/bin/cli.sh --file="${PROFILES_DIRECTORY}/${PROFILE}.cli"
else
   $JBOSS_HOME/bin/cli.sh --file="${PROFILES_DIRECTORY}/${PROFILE}.cli" >/dev/null 2>&1
fi

exit $?
