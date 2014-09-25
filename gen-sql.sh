#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

outfile=${BASE}/../cloudos-apps/apps/cloudos-dns/files/cloudos-dns.sql

${BASE}/../cloudos-lib/gen-sql.sh cloudos_dns_test ${outfile}
