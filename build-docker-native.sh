#!/bin/bash
#copy the graal certs 
#Contents/Home/lib/security/cacerts
#/Library/Java/JavaVirtualMachines/graalvm-ce-java11-19.3.1/Contents/Home
mvn clean package -Pnative -Dquarkus.native.container-build=true  -DskipTests=true
