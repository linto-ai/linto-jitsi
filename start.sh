#!/bin/bash
set -uea
. .env # Source all env

###########################################
###      Reset Jitsi Configuration      ###
###########################################

sudo rm -rf $CONFIG
mkdir -p $CONFIG/{jigasijar,web/letsencrypt,transcripts,prosody/config,/prosody/conf.d,/prosody/defaults/conf.d,prosody/prosody-plugins-custom,/prosody/data/auth%2etranscriber%2emeet%2ejitsi/accounts,jicofo,jvb,jigasi,jigasi/startup,jibri}

###########################################
###        PROSODY Configuration        ###
###########################################

envsubst < ./config/prosody/conf.d/jitsi-meet.cfg.lua> ${CONFIG}/prosody/defaults/conf.d/jitsi-meet.cfg.lua
envsubst < ./config/prosody/user/prosody-user.dat > ${CONFIG}/prosody/data/auth%2etranscriber%2emeet%2ejitsi/accounts/${JIGASI_TRANSCRIBER_XMPP_USER}.dat

###########################################
###         JIGASI Configuration        ###
###########################################

if [[ "$(docker images -q linto-jigasi:latest 2> /dev/null)" == "" ]]; then
  cd docker
  ./build-docker.sh
  cd ..
fi

cp docker/build/jigasi/target/jigasi-1.1-SNAPSHOT.jar $CONFIG/jigasijar/jigasi.jar
envsubst < ./config/jigasi/sip-communicator.properties > ${CONFIG}/jigasi/custom-sip-communicator.properties

###########################################
###           WEB Configuration         ###
###########################################

envsubst < config/web/custom-config.js > ${CONFIG}/web/custom-config.js

###########################################
###             Start Jitsi             ###
###########################################

docker stack deploy --compose-file linto-jitsi.yml linto_jitsi
