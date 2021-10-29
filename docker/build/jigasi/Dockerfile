ARG JITSI_REPO=jitsi
FROM ${JITSI_REPO}/base-java

COPY jigasi.deb /jigasi.deb

RUN \
        apt-dpkg-wrap apt-get update && \
        apt-dpkg-wrap apt-get install -y jq libxss1 ruby-hocon && \
        dpkg -i jigasi.deb && \
        apt-cleanup

RUN apt-get -y update && \ 
    apt install -y netcat && \
    apt-cleanup

COPY rootfs/ /
COPY ./wait-for-it.sh ./docker-entrypoint.sh  /
COPY ./jigasi.jar /usr/share/jigasi/jigasi.jar

VOLUME ["/config", "/tmp/transcripts"]

ENTRYPOINT ["/docker-entrypoint.sh"]

