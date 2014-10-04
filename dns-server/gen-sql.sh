#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
CLOUDOS_BASE="${BASE}/../.."

outfile=${CLOUDOS_BASE}/cloudos-apps/apps/cloudos-dns/files/cloudos-dns.sql

SILENT="${1}"
if [ ! -z "${SILENT}" ] ; then
  ${CLOUDOS_BASE}/cloudos-lib/gen-sql.sh cloudos_dns_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${CLOUDOS_BASE}/cloudos-lib/gen-sql.sh cloudos_dns_test ${outfile}
fi