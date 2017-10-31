#!/bin/bash

echo "params $*"

PROFILE="caching-service"
CLI="/opt/datagrid/bin/cli.sh"
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
   if [ ! -f "${CLI}" ]; then
      echo "CLI $CLI doesn't exist."
      exit 1
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
   shift
   shift
   ;;
   --profiles-directory)
   PROFILES_DIRECTORY="$2"
   shift
   shift
   ;;
   --cli-bin)
   CLI="$2"
   shift
   shift
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

$CLI $* --file="${PROFILES_DIRECTORY}/${PROFILE}.cli"

exit $?
