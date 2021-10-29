#!/bin/bash

###########################################
###             JIGASI Build            ###
###########################################

cd build/jigasi/

# Build source jar and debian for jigasi
# They can be found https://download.jitsi.org/unstable/
if [[ "$(docker images -q linto-jigasi-builder:latest 2> /dev/null)" == "" ]]; then
  docker build -f DockerfileBuild -t linto-jigasi-builder .
fi

# Copy source file from docker
id=$(docker create linto-jigasi-builder:latest)
docker cp $id:/jigasi_1.1-0-g9a369e3-1_amd64.deb ./jigasi.deb
docker cp $id:/jigasi/target/jigasi-2.1-0.jar ./jigasi.jar

# Build jigasi docker
docker build -t linto-jigasi .
