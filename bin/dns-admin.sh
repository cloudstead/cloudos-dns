#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)

JAR_PATTERN="cloudos-dns-*.jar"
JAR_DIR="${BASE}/../dns-server/target"

JAR="$(find ${JAR_DIR} -maxdepth 1 -type f -name ${JAR_PATTERN})"
NUM_JARS=$(find ${JAR_DIR} -maxdepth 1 -type f -name ${JAR_PATTERN} | wc -l | tr -d ' ')

if [ ${NUM_JARS} -eq 0 ] ; then
    echo 1>&2 "No cloudos-dns jar found in ${JAR_DIR}. Please build it first: cd ${BASE}/target && mvn package"
    exit 1

elif [ ${NUM_JARS} -gt 1 ] ; then
    echo 1>&2 "Multiple cloudos-dns jars found in ${JAR_DIR}: ${JAR}"
    exit 1
fi

DEBUG="${1}"
DEBUG_OPTS=""
if [ ! -z ${DEBUG} ] && [ ${DEBUG} = "debug" ] ; then
  DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5015"
  shift
fi

java ${DEBUG_OPTS} -cp ${JAR} cloudos.dns.main.DnsDirectMain "$@"
