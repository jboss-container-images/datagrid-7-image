#!/bin/sh

USE_PERFORMANCE_LOGGING=${USE_PERFORMANCE_LOGGING:-false}

adjust_java_performance_logging() {
    local options="$@"
    local additional_options="-XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions -XX:NativeMemoryTracking=summary -XX:+DisableExplicitGC"
    if [ ${USE_PERFORMANCE_LOGGING} == "true" ]; then
      echo "${options} ${additional_options}"
    else
      echo "${options}"
    fi
}
