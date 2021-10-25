#!/bin/bash
set -uea
. .env # Source all env

###########################################
###      Reset Jitsi Configuration      ###
###########################################

sudo rm -rf $LINTO_SHARED_MOUNT/jitsi
mkdir -p $LINTO_SHARED_MOUNT/jitsi/{jigasijar,web/letsencrypt,transcripts,prosody/config,/prosody/conf.d,/prosody/defaults/conf.d,prosody/prosody-plugins-custom,/prosody/data/auth%2etranscriber%2emeet%2ejitsi/accounts,jicofo,jvb,jigasi,jigasi/startup,jibri}

###########################################
###        PROSODY Configuration        ###
###########################################

envsubst < ./config/prosody/conf.d/jitsi-meet.cfg.lua> ${LINTO_SHARED_MOUNT}/jitsi/prosody/defaults/conf.d/jitsi-meet.cfg.lua
envsubst < ./config/prosody/user/prosody-user.dat > ${LINTO_SHARED_MOUNT}/jitsi/prosody/data/auth%2etranscriber%2emeet%2ejitsi/accounts/${JIGASI_TRANSCRIBER_XMPP_USER}.dat

###########################################
###         JIGASI Configuration        ###
###########################################

if [[ "$(docker images -q linto-jigasi:latest 2> /dev/null)" == "" ]]; then
  cd docker
  ./build-docker.sh
  cd ..
fi

envsubst < ./config/jigasi/sip-communicator.properties > ${LINTO_SHARED_MOUNT}/jitsi/jigasi/custom-sip-communicator.properties

###########################################
###           WEB Configuration         ###
###########################################

envsubst < config/web/custom-config.js > ${LINTO_SHARED_MOUNT}/jitsi/web/custom-config.js

###########################################
###             Start Jitsi             ###
###########################################

docker stack deploy --compose-file linto-jitsi.yml linto_jitsi
