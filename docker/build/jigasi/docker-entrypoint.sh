#!/bin/bash
set -e

sleep 15
echo "Waiting prosody..."
/wait-for-it.sh linto_jitsi_prosody:5280 --timeout=25 --strict -- echo " linto_jitsi_prosody:5280 is up"

eval "./init"
