#!/bin/bash

DEPLOY=${1}
if [ -z ${DEPLOY} ] ; then
  echo "Usage $0 <deploy-dir>"
  exit 1
fi

BASE=$(cd $(dirname $0) && pwd)
CLOUDOS_BASE="$(cd ${BASE}/../.. && pwd)"
CLOUDOS_APPS="${CLOUDOS_BASE}/cloudos-apps"

cp install/* ${DEPLOY}/
cp bin/cdns ${DEPLOY}/
cp ${CLOUDOS_APPS}/apps/cloudos-dns/files/cloudos-dns.sql ${DEPLOY}/
cp ${CLOUDOS_APPS}/apps/java/files/jrun* ${DEPLOY}/
