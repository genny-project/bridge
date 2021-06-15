#!/bin/bash
project=`echo "${PWD##*/}" | tr '[:upper:]' '[:lower:]'`
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
#version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
else
  version="${1}"
fi


USER=`whoami`
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true
docker tag ${USER}/${project}:${version} ${org}/${project}:${version}
docker tag ${USER}/${project}:${version} ${org}/${project}:latest
