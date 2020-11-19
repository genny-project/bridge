#!/bin/bash
#project=${PWD##*/}
#file="src/main/resources/${project}-git.properties"
#org=gennyproject
#function prop() {
#  grep "${1}=" ${file} | cut -d'=' -f2
#}
#version=$(prop 'git.build.version')


#./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true
#docker tag ${USER}/${project}:${version} ${org}/${project}:${version}
#docker tag ${USER}/${project}:${version} ${org}/${project}:latest

project=bridge
file="src/main/resources/${project}-git.properties"

function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}

if [ -z "${1}" ]; then
  version="latest"
else
  version="${1}"
fi

if [ -f "$file" ]; then
  echo "$file found."
  echo "git.commit.id = " "$(prop 'git.commit.id')"
  echo "git.build.version = " "$(prop 'git.build.version')"
  docker build -t gennyproject/${project}:"${version}" .
else
  echo "ERROR: git properties $file not found."
fi

image_ids=$(docker images | grep ${project} | grep none)
if [ "${image_ids:-0}" == 0 ]; then
  echo 'Skip clean up'
else
  docker images | grep ${project} | grep none | awk '{print $3}' | xargs docker rmi
fi
