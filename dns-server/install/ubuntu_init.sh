#!/bin/bash
#
# If installing cloudos-dns standalone on Ubuntu, this script will install all the dependencies
# required to run install_standalone.sh
#

function die () {
  echo 2>&1 "${1}"
  exit 1
}

if [ $(whoami) != "root" ] ; then
  die "Must be root"
fi

apt-get update
apt-get install -y postgresql memcached redis-server openjdk-7-jre-headless curl uuid
