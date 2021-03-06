#!/bin/bash
set -e

sleep 15
echo "Waiting prosody..."
/wait-for-it.sh $LINTO_STACK_PROSODY_HOST:$LINTO_STACK_PROSODY_PORT --timeout=25 --strict -- echo " $LINTO_STACK_PROSODY_HOST:$LINTO_STACK_PROSODY_PORT is up"

# Fix jigash.sh is not exacutable on with bebian build
chmod +x /usr/share/jigasi/jigasi.sh

eval "./init"
