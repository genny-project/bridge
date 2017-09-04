#FROM openjdk:8u131-jre-alpine
#FROM openjdk:9-b181-jre
#RUN apk update && apk add jq && apk add bash

FROM adamcrow64/javamvn

USER root

#RUN apk update && apk add jq && apk add bash
RUN set -x \
    && apt-get update --quiet \
    && apt-get install --quiet --yes --no-install-recommends jq bash iputils-ping vim  \
    && apt-get clean



ADD target/bridge-0.0.1-SNAPSHOT-fat.jar /service.jar
ADD cluster.xml /cluster.xml

RUN mkdir /realm
ADD realm /opt/realm
ADD docker-entrypoint.sh /docker-entrypoint.sh

WORKDIR /

EXPOSE 5701
EXPOSE 5702
EXPOSE 8081
EXPOSE 15701

#CMD ["java"]
ENTRYPOINT [ "/docker-entrypoint.sh" ]

