#!/bin/sh

USE_OFF_HEAP=${USE_OFF_HEAP:-true}
XMX_FOR_OFF_HEAP_MB=52
JVM_NATIVE_MB=25

function prepareEnv() {
  unset EVICTION_TOTAL_MEMORY_B
}

function configure() {
   if [ ${USE_OFF_HEAP} == "true" ]; then
       EVICTION_TOTAL_MEMORY_B=$(expr ${CONTAINER_MAX_MEMORY} - ${JVM_NATIVE_MB} * 1000000 - ${XMX_FOR_OFF_HEAP_MB} * 1000000)
       export EVICTION_TOTAL_MEMORY_B
   fi
}

function source_java_run_scripts() {
    local java_scripts_dir="/opt/run-java"
    # set CONTAINER_MAX_MEMORY and CONTAINER_CORE_LIMIT
    source "${java_scripts_dir}/container-limits"
    # load java options functions
    source "${java_scripts_dir}/java-default-options"
}

source_java_run_scripts

# Returns a set of options that are not supported by the current jvm.  The idea
# is that java-default-options always configures settings for the latest jvm.
# That said, it is possible that the configuration won't map to previous
# versions of the jvm.  In those cases, it might be better to have different
# implementations of java-default-options for each version of the jvm (e.g. a
# private implementation that is sourced by java-default-options based on the
# jvm version).  This would allow for the defaults to be tuned for the version
# of the jvm being used.
unsupported_options() {
    if [[ $($JAVA_HOME/bin/java -version 2>&1 | awk -F "\"" '/version/{ print $2}') == *"1.7"* ]]; then
        echo "(-XX:NativeMemoryTracking=[^ ]*|-XX:+PrintGCDateStamps|-XX:+UnlockDiagnosticVMOptions|-XX:CICompilerCount=[^ ]*|-XX:GCTimeRatio=[^ ]*|-XX:MaxMetaspaceSize=[^ ]*|-XX:AdaptiveSizePolicyWeight=[^ ]*)"
    else
        echo "(--XX:MaxPermSize=[^ ]*)"
    fi
}


# Merge default java options into the passed argument
adjust_java_options() {
    local options="$@"
    local remove_xms
    local java_scripts_dir="/opt/run-java"
    local java_options=$(source "${java_scripts_dir}/java-default-options")
    local unsupported="$(unsupported_options)"

    # Off-heap requires a fixed amount of heap memory. The rest is stored off-heap.
    # From our measurements it turned out that 52M is enough.
    if [ ${USE_OFF_HEAP} == "true" ]; then
      java_options=$(echo ${java_options} | sed -e "s/-Xmx[^ ]*/${option}/")
      java_options=$(echo ${java_options} | sed -e "s/-Xms[^ ]*/${option}/")
      java_options="-Xmx${XMX_FOR_OFF_HEAP_MB}M -Xms${XMX_FOR_OFF_HEAP_MB}M ${java_options}"
    fi

    for option in $java_options; do
        if [[ ${option} == "-Xmx"* ]]; then
            if [[ "$options" == *"-Xmx"* ]]; then
                options=$(echo $options | sed -e "s/-Xmx[^ ]*/${option}/")
            else
                options="${options} ${option}"
            fi
            if [ "x$remove_xms" == "x" ]; then
                remove_xms=1
            fi
        elif [[ ${option} == "-Xms"* ]]; then
            if [[ "$options" == *"-Xms"* ]]; then
                options=$(echo $options | sed -e "s/-Xms[^ ]*/${option}/")
            else
                options="${options} ${option}"
            fi
            remove_xms=0
        elif $(echo "$options" | grep -Eq -- "${option%=*}(=[^ ]*)?(\s|$)") ; then
            options=$(echo $options | sed -re "s@${option%=*}(=[^ ]*)?(\s|$)@${option}\2@")
        else
            options="${options} ${option}"
        fi
    done

    if [[ "x$remove_xms" == "x1" ]]; then
        options=$(echo $options | sed -e "s/-Xms[^ ]*/ /")
    fi

    options=$(echo "${options}"| sed -re "s@${unsupported}(\s)?@@g")
    echo "${options}"
}
