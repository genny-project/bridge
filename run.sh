#!/bin/bash

MYIP=''
GetIP()
{
   MYIP=
   while IFS=$': \t' read -a line ;do
       [ -z "${line%inet}" ] && ip=${line[${#line[1]}>4?1:2]} &&
           [ "${ip#127.0.0.1}" ] && MYIP=$ip
     done< <(LANG=C /sbin/ifconfig $1)
}

CheckInterface(){
   ifconfig $1 > /dev/null 2>&1
}

CheckInterface eth0 && GetIP eth0 || CheckInterface en0  && GetIP en0

export MYIP=$MYIP

ADMIN_PASSWORD=GENNY
ADMIN_USERNAME=GENNY
export IS_CACHE_SERVER=true
export CACHE_SERVER_NAME=$MYIP

export USER=GENNY
java -jar -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} target/bridge-7.9.0-runner.jar
