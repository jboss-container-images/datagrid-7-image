#!/bin/sh

OUTPUT=/tmp/liveness-output
ERROR=/tmp/liveness-error
LOG=/tmp/liveness-log

COUNT=1
SLEEP=1
DEBUG_SCRIPT=false
PROBE_IMPL="probe.jdg.jolokia.JdgProbe"

if [ $# -gt 0 ] ; then
    COUNT=$1
fi

if [ $# -gt 1 ] ; then
    SLEEP=$2
fi

if [ $# -gt 2 ] ; then
    DEBUG_SCRIPT=$3
fi

if [ $# -gt 3 ] ; then
    PROBE_IMPL=$4
fi

if [ "$DEBUG_SCRIPT" = "true" ]; then
    DEBUG_OPTIONS="--debug --logfile $LOG --loglevel DEBUG"
fi

if python $JBOSS_HOME/bin/probes/runner.py -c READY -c NOT_READY --maxruns $COUNT --sleep $SLEEP $DEBUG_OPTIONS $PROBE_IMPL; then
    exit 0
fi
exit 1

