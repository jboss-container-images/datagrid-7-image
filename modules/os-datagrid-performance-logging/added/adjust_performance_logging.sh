#!/bin/sh

adjust_java_performance_logging() {
    local options="$@"
    local additional_options="-XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions -XX:NativeMemoryTracking=summary -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdumps"
    if [ "${DEBUG}" == "true" ]; then
      echo "${options} ${additional_options}"
    else
      echo "${options}"
    fi
}
