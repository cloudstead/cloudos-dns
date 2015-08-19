#!/bin/bash

function die () {
  echo 2>&1 "${1}"
  exit 1
}

if [ $(whoami) != "root" ] ; then
  die "Must be root"
fi

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

apt-get install -y daemon unzip

kestrel_user='kestrel'
kestrel_home='/usr/local/kestrel'

useradd -r -d ${kestrel_home} -s /bin/bash ${kestrel_user}

for dir in /usr/local /var/log /var/run /var/spool ; do
  mkdir -p ${dir} || die "Error creating ${dir}"
  chown -R ${kestrel_user} ${dir} && chmod 755 ${dir} || die "Error setting permissions on ${dir}"
done

touch /etc/kestrel.env # not sure we need this at all

curl -o /tmp/kestrel-2.4.1.zip 'http://cloudstead.io/downloads/kestrel-2.4.1.zip'

cd ${kestrel_home}
unzip /tmp/kestrel-2.4.1.zip
ln -s kestrel-2.4.1 current
chmod +x current/scripts/*

# Make it look like a regular jrun service so we can start/stop/status it with jrun-init
mkdir -p ${kestrel_home}/logs
mkdir -p ${kestrel_home}/target
cd ${kestrel_home}/target
KESTREL_JAR=$(find ../current/ -type f -name "kestrel*.jar" | grep -v javadoc | grep -v sources | grep -v test)
if [ -z ${KESTREL_JAR} ] ; then
  echo "Kestrel jar not found"
  exit 1
fi
ln -s ${KESTREL_JAR} kestrel-$(basename ${KESTREL_JAR})
ln -s ../current/config
cd ${kestrel_home}

chown -R ${kestrel_user} ${kestrel_home}

cp ${BASE}/kestrel /etc/init.d/kestrel
service kestrel start
