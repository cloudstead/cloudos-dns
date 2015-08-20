#!/bin/bash

function die () {
  echo 2>&1 "${1}"
  exit 1
}

if [ $(whoami) != "root" ] ; then
  die "Must be root"
fi

if [ -z "$(which java)" ] ; then
  die "Please install Java 7"
fi
if [ $(java -version | grep '1.7' | wc -l | tr -d ' ') -eq 0 ] ; then
  die "Please install Java 7. Found Java: $(java -version)"
fi

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

# Copy jrun files to /usr/local/bin
cp ${BASE}/jrun* /usr/local/bin || die "Error copying jrun utilities"
chmod a+rx /usr/local/bin/jrun* || die "Error setting permissions on jrun utilities"

if [ -z "$(which unzip)" ] ; then
  if [ ! -z "$(which apt-get)" ] ; then
    apt-get install -y unzip
  elif [ ! -z "$(which yum)" ] ; then
    yum install unzip
  else
    die "Please install unzip"
  fi
fi
if [ -z "$(which unzip)" ] ; then
  if [ ! -z "$(which apt-get)" ] ; then
    apt-get install -y daemon || die "Error installing daemon. Please install daemon from: http://libslack.org/daemon/"
  elif [ ! -z "$(which yum)" ] ; then
    yum install daemon || die "Error installing daemon. Please install daemon from: http://libslack.org/daemon/"
  else
    die "Please install daemon from: http://libslack.org/daemon/"
  fi
fi

kestrel_user='kestrel'
kestrel_home='/usr/local/kestrel'

useradd -r -d ${kestrel_home} -s /bin/bash ${kestrel_user}

for dir in /usr/local /var/log /var/run /var/spool ; do
  mkdir -p ${dir}/kestrel || die "Error creating ${dir}"
  chown -R ${kestrel_user} ${dir}/kestrel && chmod 755 ${dir}/kestrel || die "Error setting permissions on ${dir}"
done

touch /etc/kestrel.env # not sure we need this at all

curl -o /tmp/kestrel-2.4.1.zip 'http://cloudstead.io/downloads/kestrel-2.4.1.zip'

mkdir -p ${kestrel_home} && cd ${kestrel_home} || die "Error creating ${kestrel_home}"
unzip /tmp/kestrel-2.4.1.zip
ln -s ${kestrel_home}/kestrel-2.4.1 ${kestrel_home}/current
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
