#!/bin/bash

function die () {
  echo "${1}" && exit 1
}

BASE=$(cd $(dirname $0) && pwd)

JAR="${BASE}/../target/cloudos-dns-server-*.jar"

MOCK_RESOURCE="cloudos/dns/resources/MockDnsResource.class"
TEST_CLASSES="$(cd ${BASE}/../target/test-classes && pwd)"
BASE_MOCK_RESOURCE="${TEST_CLASSES}/${MOCK_RESOURCE}"

if [[ $(ls ${JAR} | wc -l | tr -d ' ')  -eq 0  ]] || [[ ! -f ${BASE_MOCK_RESOURCE} ]] ; then
echo "not found: ${BASE_MOCK_RESOURCE} or ${JAR}" && exit 1
  pushd ${BASE}/..
  mvn -DskipTests=true package test || die "Error building cloudos-dns-server jar"
  popd
fi
if [[ $(ls ${JAR} | wc -l | tr -d ' ')  -eq 0  ]] || [[ ! -f ${BASE_MOCK_RESOURCE} ]] ; then
  die "Error building cloudos-dns-server jar"
fi

TEMPDIR=$(mktemp -d /tmp/$0.XXXXXXX) || die "Error creating temp dir"
cd ${TEMPDIR} && \
  cp ${JAR} ${TEMPDIR} && \
  cp ${BASE}/dns-config.yml ${TEMPDIR} && \
  jar uvf ${JAR} dns-config.yml && \
  cd ${TEST_CLASSES} && \
  jar uvf ${JAR} ${MOCK_RESOURCE} && \
  cp ${JAR} ${BASE}/../target/dns-server-mock.jar

