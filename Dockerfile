#FROM openjdk:8u212-jre-alpine3.9 
FROM adoptopenjdk/openjdk11:alpine
RUN mv /usr/glibc-compat/lib/ld-linux-x86-64.so.2 /usr/glibc-compat/lib/ld-linux-x86-64.so
RUN ln -s /usr/glibc-compat/lib/ld-linux-x86-64.so /usr/glibc-compat/lib/ld-linux-x86-64.so.2

RUN apk update && apk add jq && apk add curl && apk add bash && apk add xmlstarlet && apk add wget && apk add vim && apk add unzip && apk add sed
RUN ln -s /bin/sed /usr/bin/sed
RUN chmod a+x /usr/bin/sed

RUN apk update && apk add jq && apk add curl && apk add bash

#RUN apk add --no-cache libc6-compat gcompat

EXPOSE 10001


COPY target/lib/* /deployments/lib/
COPY target/*-runner.jar /deployments/service.jar

#RUN mkdir /realm
#ADD realm /opt/realm
ADD docker-entrypoint.sh /docker-entrypoint.sh

WORKDIR /

EXPOSE 5701
EXPOSE 5702
EXPOSE 5703
EXPOSE 5704
EXPOSE 5705
EXPOSE 5706
EXPOSE 8088
EXPOSE 15701

HEALTHCHECK --interval=10s --timeout=3s --retries=15 CMD curl -f / http://localhost:8088/version || exit 1

#CMD ["java"]
ENTRYPOINT [ "/docker-entrypoint.sh" ]

