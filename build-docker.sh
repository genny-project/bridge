#!/bin/bash

#project=${PWD##*/}
#project=bridge
#realm=gennyproject
#version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
#echo "project ${project}"
#echo "version ${version}"
#branch=$(git rev-parse --symbolic-full-name --abbrev-ref HEAD)
#commit_hash=$(git rev-parse HEAD)
#
#if [ -z "${1}" ]; then
#  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
#else
#  version="${1}"
#fi
#
## build docker imges
#echo "Git branch:" "${branch}"
#echo "Last git commit hash:" "${commit_hash}"
#docker  build --no-cache -f src/main/docker/Dockerfile.fast-jar  -t  ${realm}/${project}:latest .
#
##clean up
#image_ids=$(docker images | grep ${project} | grep none)
#if [ "${image_ids:-0}" == 0 ]; then
#  echo 'Skip clean up'
#else
#  docker images | grep ${project} | grep none | awk '{print $3}' | xargs docker rmi
#fi
#
#docker tag ${realm}/${project}:latest ${realm}/${project}:${version} 



project=${PWD##*/}
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
