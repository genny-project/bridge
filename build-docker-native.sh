#!/bin/bash

./build-native.sh
./mvnw package -Pnative -Dquarkus.native.container-build=true -DskipTests=true

