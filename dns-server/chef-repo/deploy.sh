#!/bin/bash
#
# Usage: ./deploy.sh user@host [solo-json] [tempdir|inline]
#
# user@host: target to deploy. user must have password-less sudo privileges.
# solo-json: path to a Chef run list (typically solo.json) to use. Otherwise a minimal one is generated based on defaults.
# mode: default is 'tempdir' which will create a new temp dir with init files added. 'inline' will copy init files into this chef repo.
#
# Optional environment variables:
#
#   INIT_FILES  -- if set, a directory containing data bags and certs for the chef-run.
#                  if not set, script checks for a directory named "init_files" in the current directory
#
#   SSH_KEY     -- if set, the path to the private key to use when connecting
#                  if not set, script checks for existence of ~/.ssh/id_dsa or ~/.ssh/id_rsa (in that order)
#
#   REQUIRED    -- space-separated list of required files, typically databags and certificates
#
#   COOKBOOK_SOURCES -- dirs to look for cookbooks in
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
CLOUDOS_BASE=$(cd ${BASE}/../../.. && pwd)

DEPLOYER=${BASE}/deploy_lib.sh
if [ ! -x ${DEPLOYER} ] ; then
  DEPLOYER=${CLOUDOS_BASE}/cloudos-lib/chef-repo/deploy_lib.sh
  if [ ! -x ${DEPLOYER} ] ; then
    die "ERROR: deployer not found or not executable: ${DEPLOYER}"
  fi
fi

host="${1:?no user@host specified}"
SOLO_JSON="${2:-solo.json}"
MODE="${3:-tempdir}"

# SSH key
DEFAULT_DSA_KEY="${HOME}/.ssh/id_dsa"
DEFAULT_RSA_KEY="${HOME}/.ssh/id_rsa"
if [ -z ${SSH_KEY} ] ; then
  SSH_KEY="${DEFAULT_DSA_KEY}"
  if [ ! -f "${SSH_KEY}" ] ; then
    SSH_KEY="${DEFAULT_RSA_KEY}"
    if [ ! -f "${SSH_KEY}" ] ; then
      die "SSH_KEY environment variable was not defined and neither ${DEFAULT_DSA_KEY} nor ${DEFAULT_RSA_KEY} exists"
    fi
  fi
fi

# init files
if [ -z "${INIT_FILES}" ] ; then
  INIT_FILES="${BASE}/init_files"
fi
if [ ! -d "${INIT_FILES}" ] ; then
  die "No init_files configuration found in ${INIT_FILES}"
fi

function append_recipe () {
  local json="$1"
  local recipe="$2"
  local TMP=$(mktemp /tmp/append_recipe.XXXXXX) || die "append_recipe: error creating temp file"
  ${JSON_EDIT} -f ${json} -o write -p run_list[] -v \"${recipe}\" -w ${TMP} > /dev/null 2>&1 || die "append_recipe: error appending ${recipe} to ${json}"
  echo ${TMP}
}

if [ -z "${REQUIRED}" ] ; then
  REQUIRED=" \
data_bags/cloudos-dns/init.json \
data_bags/cloudos-dns/ports.json \
data_bags/djbdns/init.json \
certs/cloudos-dns/ssl-https.key \
certs/cloudos-dns/ssl-https.pem \
"
fi

if [ -z "${COOKBOOK_SOURCES}" ] ; then
  COOKBOOK_SOURCES="$(find ${CLOUDOS_BASE}/cloudos-apps/apps -type d -name cookbooks)"
fi

${DEPLOYER} ${host} ${INIT_FILES} "${REQUIRED}" "${COOKBOOK_SOURCES}" ${SOLO_JSON} ${MODE}
