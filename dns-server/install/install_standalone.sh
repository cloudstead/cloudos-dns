#!/bin/bash
#
# Install cloudos-dns as a minimal standalone server.
# Use this when installing cloudos-dns on an existing djbdns server.
#
# Edit install.env to customize the installation.
#
# This script is typically run from within the base directory of an unrolled cloudos-dns tarball,
# so paths are relative to that structure. See prep-deploy.sh for how this directory structure is created.
#

function die () {
  echo 2>&1 "${1}"
  exit 1
}

if [ $(whoami) != "root" ] ; then
  die "Must be root"
fi

if [[ ! $(service cloudos-dns status 2>&1) =~ "unrecognized" ]] ; then
  echo "cloudos-dns already installed"
  echo "service cloudos-dns status = $(service cloudos-dns status)"
  exit 0
fi

if [ -z "$(which java)" ] ; then
  die "Please first install Java (version 7 or higher)"
fi
if [ -z "$(which psql)" ] ; then
  die "Please first install PostgreSQL (psql was not found on the PATH: ${PATH})"
fi
if [ -z "$(which createuser)" ] ; then
  die "Please first install PostgreSQL (createuser was not found on the PATH: ${PATH})"
fi
if [ -z "$(which createdb)" ] ; then
  die "Please first install PostgreSQL (createdb was not found on the PATH: ${PATH})"
fi
if [ -z "$(which redis-server)" ] ; then
  die "Please first install Redis (redis-server was not found on the PATH: ${PATH})"
fi

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
if [ ! -f install.env ] ; then
  die "No $(pwd)/install.env file found"
fi

. install.env

if ! id postgres > /dev/null 2>&1 ; then
  die "Postgres user (${PG_USER}) was not found. Please edit install.env and set PG_USER"
fi

if [ -z "${CLOUDOS_DNS_USER}" ] ; then
  die "No CLOUDOS_DNS_USER defined in install.env"
fi
if [ -z "${CLOUDOS_ADMIN_USER}" ] ; then
  die "No CLOUDOS_ADMIN_USER defined in install.env"
fi
if [ -z "${CLOUDOS_ADMIN_PASS}" ] ; then
  die "No CLOUDOS_ADMIN_PASS defined in install.env"
fi

if [[ -z "${DNS_DYN}" && -z "${DNS_HANDLER}" ]] ; then
  die "Neither DNS_DYN nor DNS_HANDLER was defined in install.env (please define one of them)"
elif [[ ! -z "${DNS_DYN}" && ! -z "${DNS_HANDLER}" ]] ; then
  die "Both DNS_DYN and DNS_HANDLER were defined in install.env (please define only one of them)"
fi

DJBDNS_DATA_FILE="/etc/tinydns/root/data"
if [[ ! -z "${DNS_DJB}" && ! -f ${DJBDNS_DATA_FILE} ]] ; then
  die "djbdns was selected but no data file was found in ${DJBDNS_DATA_FILE}. Please install and configure djbdns"
fi

# Create system user
if ! id ${CLOUDOS_DNS_USER} > /dev/null 2>&1 ; then
  useradd -m ${CLOUDOS_DNS_USER} -s /usr/sbin/nologin || die "Error creating user: ${CLOUDOS_DNS_USER}"
fi
CLOUDOS_DNS_HOME=$(bash -c "echo -n ~${CLOUDOS_DNS_USER}")

# Copy files to ${CLOUDOS_DNS_HOME}/cloudos-dns
export SERVER_ROOT="${CLOUDOS_DNS_HOME}/cloudos-dns"
mkdir -p ${SERVER_ROOT}/logs || die "Error creating directories"
rsync -avzc ${BASE}/target ${SERVER_ROOT} || die "Error copying files to ${SERVER_ROOT}"

# Write the .cloudos-dns.env file if it does not exist
ENV_FILE="${CLOUDOS_DNS_HOME}/.cloudos-dns.env"
if [[ -f ${ENV_FILE} && $(cat ${ENV_FILE} | wc -w | tr -d ' ') -gt 0 ]] ; then
  # Source existing non-empty file, it will override install.env
  . ${ENV_FILE}

else
  if [[ ! -z "${DNS_DYN}" ]] ; then
    cat > ${ENV_FILE} <<ENVDATA
export SESSION_DATAKEY=$(head -c 16 /dev/urandom | od -x -A none | tr -d ' ')
export PUBLIC_BASE_URI=${PUBLIC_BASE_URI}
export CLOUDOS_DNS_DB_USER=${CLOUDOS_DNS_DB_USER}
export CLOUDOS_DNS_DB_PASS=${CLOUDOS_DNS_DB_PASS}
export CLOUDOS_DNS_DB_NAME=${CLOUDOS_DNS_DB_NAME}
export CLOUDOS_DNS_DB_HOST=${CLOUDOS_DNS_DB_HOST}
export CLOUDOS_DNS_DB_PORT=${CLOUDOS_DNS_DB_PORT}
export CLOUDOS_DNS_SERVER_PORT=${CLOUDOS_DNS_SERVER_PORT}

# Dyn is enabled
export DNS_DYN=true
export DYNDNS_ACCOUNT=${DYNDNS_ACCOUNT}
export DYNDNS_USER=${DYNDNS_USER}
export DYNDNS_PASSWORD=${DYNDNS_PASSWORD}
export DYNDNS_ZONE=${DYNDNS_ZONE}
ENVDATA
  else
    cat > ${ENV_FILE} <<ENVDATA
export SESSION_DATAKEY=$(head -c 16 /dev/urandom | od -x -A none | tr -d ' ')
export PUBLIC_BASE_URI=${PUBLIC_BASE_URI}
export CLOUDOS_DNS_DB_USER=${CLOUDOS_DNS_DB_USER}
export CLOUDOS_DNS_DB_PASS=${CLOUDOS_DNS_DB_PASS}
export CLOUDOS_DNS_DB_NAME=${CLOUDOS_DNS_DB_NAME}
export CLOUDOS_DNS_DB_HOST=${CLOUDOS_DNS_DB_HOST}
export CLOUDOS_DNS_DB_PORT=${CLOUDOS_DNS_DB_PORT}
export CLOUDOS_DNS_SERVER_PORT=${CLOUDOS_DNS_SERVER_PORT}

export ROOTY_QUEUE_NAME=${ROOTY_QUEUE_NAME}
export ROOTY_SECRET=${ROOTY_SECRET}

# Dyn is not enabled
# export DNS_DYN=

# local DNS is enabled
export DNS_HANDLER=${DNS_HANDLER}
ENVDATA
  fi
fi
chown ${CLOUDOS_DNS_USER} ${ENV_FILE} && chmod 600 ${ENV_FILE} || die "Error setting permissions on ${ENV_FILE}"

# Copy jrun files to /usr/local/bin
cp ${BASE}/jrun* /usr/local/bin || die "Error copying jrun utilities"
chmod a+rx /usr/local/bin/jrun* || die "Error setting permissions on jrun utilities"

# Install cdns script
mkdir -p ${SERVER_ROOT} && cp ${BASE}/cdns ${SERVER_ROOT} && chmod a+rx ${SERVER_ROOT}/cdns || die "Error copying cdns utility"
cd /usr/local/bin
rm -f cdns
ln -s ${SERVER_ROOT}/cdns
cd ${BASE}

# Initialize the database
export PSQL='psql --quiet --tuples-only'
export PSQL_PG="psql -U ${PG_USER} --quiet --tuples-only"
export PSQL_COMMAND="${PSQL} --command"
export PSQL_PG_COMMAND="${PSQL_PG} --command"
export USER_PSQL="${PSQL} -U ${CLOUDOS_DNS_DB_USER} -h ${CLOUDOS_DNS_DB_HOST} -p ${CLOUDOS_DNS_DB_PORT}"

# check for existing password in ~cloudos-dns
export SCHEMA_FILE="${BASE}/cloudos-dns.sql"

if [ ! -z "$(which uuid 2> /dev/null)" ] ; then
  uuid=$(uuid -v 4)
elif [ ! -z "$(which uuidgen 2> /dev/null)" ] ; then
  uuid=$(uuidgen -r)
else
  die "No UUID program found!"
fi

su -l -p ${PG_USER} <<INITDB

# Create database user
DB_USER_EXISTS=$(${PSQL_PG_COMMAND} "select usename from pg_user" | grep ${CLOUDOS_DNS_DB_USER} | wc -l | tr -d ' ')
if [ ${DB_USER_EXISTS} -eq 0 ] ; then
  createuser --no-createdb --no-createrole --no-superuser ${CLOUDOS_DNS_DB_USER}
  echo "ALTER USER ${CLOUDOS_DNS_DB_USER} PASSWORD '"${CLOUDOS_DNS_DB_PASS}"'" | ${PSQL_PG}
fi

# Create database
DB_EXISTS=$(${PSQL_PG_COMMAND} "select datname from pg_database" | grep ${CLOUDOS_DNS_DB_NAME} | wc -l | tr -d ' ')
if [ ${DB_EXISTS} -eq 0 ] ; then
  createdb --encoding=UNICODE --owner=${CLOUDOS_DNS_DB_USER} ${CLOUDOS_DNS_DB_NAME}
fi

# Populate database
SCHEMA_EXISTS=$(PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" ${USER_PSQL} -c "select tableowner from pg_tables" ${CLOUDOS_DNS_DB_NAME} | grep ${CLOUDOS_DNS_DB_USER} | wc -l | tr -d ' ')
if [ ${SCHEMA_EXISTS} -eq 0 ] ; then
  PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" ${USER_PSQL} -f ${SCHEMA_FILE} ${CLOUDOS_DNS_DB_NAME}
fi

# Create admin user
jar=$(find ${SERVER_ROOT}/target -type f -name cloudos-dns-server-*.jar | head -1)
ADMIN_EXISTS=$(PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" ${USER_PSQL} -c "select count(*) from dns_account where admin = true" ${CLOUDOS_DNS_DB_NAME} | head -1 | tr -d ' ')
if [ ${ADMIN_EXISTS} -eq 0 ] ; then
  ADMIN_PASS_BCRYPTED=$(java -cp ${jar} org.cobbzilla.util.security.bcrypt.BCryptUtil 12 ${CLOUDOS_ADMIN_PASS})
  PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" ${USER_PSQL} -c \
    "insert into dns_account (uuid, ctime, admin, name, hashed_password) VALUES ('"${uuid}"', 0, TRUE, '"${CLOUDOS_ADMIN_USER}"', '"${ADMIN_PASS_BCRYPTED}"')"  ${CLOUDOS_DNS_DB_NAME}
fi
INITDB

# Write the init.d service files
SERVER_INIT="/etc/init.d/cloudos-dns"
cat ${BASE}/cloudos-dns | sed -e "s/@@USER@@/${CLOUDOS_DNS_USER}/g" > ${SERVER_INIT} || die "Error writing init.d service file: ${SERVER_INIT}"
chmod 755 ${SERVER_INIT} || die "Error setting permission on init.d service file: ${SERVER_INIT}"

# Install rooty if using local DNS handler
if [ ! -z "${DNS_HANDLER}" ] ; then
  if [ ! -e /etc/init.d/kestrel ] ; then
    ${BASE}/install_kestrel.sh
  fi

  ROOTY_INIT="/etc/init.d/cloudos-dns-rooty"
  if [ ! -f ${ROOTY_INIT} ] ; then
    cat ${BASE}/cloudos-dns-rooty | sed -e "s/@@USER@@/${CLOUDOS_DNS_USER}/g" > ${ROOTY_INIT} || die "Error writing init.d service file: ${ROOTY_INIT}"
    chmod 755 ${ROOTY_INIT} || die "Error setting permission on init.d service file: ${ROOTY_INIT}"

    ROOTY_CONFIG="/etc/rooty.yml"
    cat ${BASE}/rooty.yml \
      | sed -e "s/@@ROOTY_QUEUE_NAME@@/${ROOTY_QUEUE_NAME}/" \
      | sed -e "s/@@ROOTY_SECRET@@/${ROOTY_SECRET}/" \
      | sed -e "s/@@DNS_HANDLER@@/${DNS_HANDLER}/" \
      > ${ROOTY_CONFIG}

    service cloudos-dns-rooty start
    sleep 5s
    num_procs=$(ps auxwwwww | egrep -- 'rooty.RootyMain' | grep -v egrep | wc -l | tr -d ' ')
    if [ ${num_procs} -eq 0 ] ; then
      die "cloudos-dns-rooty service did not start correctly"
    fi
  fi
fi

# Set permissions
chown -R ${CLOUDOS_DNS_USER} ${SERVER_ROOT} || die "Error setting ownership on ${SERVER_ROOT}"

# Start the service
service cloudos-dns restart

# Ensure it is running
start=$(date +%s)
timeout=60
local_endpoint="http://127.0.0.1:${CLOUDOS_DNS_SERVER_PORT}/api/dns"
while [ $(expr $(date +%s) - ${start}) -lt ${timeout} ] ; do
  http_status="$(curl -sL -w "%{http_code}" --max-time 10 ${local_endpoint} -o /dev/null)"
  rval=$?
  http_status_type=$(expr ${http_status} / 100)
  if [[ ${rval} -eq 0 && ( ${http_status_type} == 2 || ${http_status_type} == 3 ) ]] ; then
    echo "cloudos-dns successfully installed and running on ${local_endpoint}. Please use Apache or nginx to proxy external requests (via https) to it."
    exit 0
  else
    echo "Still waiting for cloudos-dns to be running..."
    sleep 5s
  fi
done

die "cloudos-dns did not start properly. Look in ${SERVER_ROOT}/logs for more information."
