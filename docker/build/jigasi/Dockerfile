ARG JITSI_REPO=jitsi
FROM ${JITSI_REPO}/base-java

COPY jigasi_1.1-169-g4f05fbd-1_amd64.deb /jigasi_1.1-169-g4f05fbd-1_amd64.deb

RUN \
        apt-dpkg-wrap apt-get update && \
        apt-dpkg-wrap apt-get install -y jq libxss1 ruby-hocon && \
        dpkg -i jigasi_1.1-169-g4f05fbd-1_amd64.deb && \
        apt-cleanup

RUN apt-get -y update && \ 
    apt-get -y install netcat && \
    apt-cleanup

COPY rootfs/ /
COPY ./wait-for-it.sh /
COPY ./docker-entrypoint.sh /

VOLUME ["/config", "/tmp/transcripts"]

ENTRYPOINT ["/docker-entrypoint.sh"]

