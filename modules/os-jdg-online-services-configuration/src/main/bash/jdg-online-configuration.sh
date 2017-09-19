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

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case $PARAM in
        -h | --help)
            usage
            exit
            ;;
        --profile)
            PROFILE=$VALUE
            ;;
        --profiles-directory)
            PROFILES_DIRECTORY=$VALUE
            ;;
        --cli-bin)
            CLI=$VALUE
            ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            usage
            exit 1
            ;;
    esac
    shift
done

check_if_profile_exists
check_if_cli_exists

$CLI --file="${PROFILES_DIRECTORY}/${PROFILE}.cli"

exit $?
