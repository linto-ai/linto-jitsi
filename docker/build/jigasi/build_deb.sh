#!/bin/bash
set -x
set -e

# Make sure you have the following environment variables set, example:
# export DEBFULLNAME="Jitsi Team"
# export DEBEMAIL="dev@jitsi.org"
# You need package devscripts installed (command dch).

echo "==================================================================="
echo "   Building DEB packages...   "
echo "==================================================================="

SCRIPT_FOLDER=$(dirname "$0")
# cd "$SCRIPT_FOLDER/.."

# Let's get version from maven
MVNVER=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
TAG_NAME="v${MVNVER/-SNAPSHOT/}"

echo "Current tag name: $TAG_NAME"

VERSION_FULL="1"
echo "Full version: ${VERSION_FULL}"

VERSION="2"
echo "Package version: ${VERSION}"

REV="3"
dch -v "$VERSION-1" "Build from git. $REV"
dch -D unstable -r ""

# We need to make sure all dependencies are downloaded before start building
# the debian package
mvn dependency:resolve

# sets the version in the pom file so it will propagte to resulting jar
mvn versions:set -DnewVersion="${VERSION}"

# now build the deb
dpkg-buildpackage -tc -us -uc -b -d -aamd64

# clean the current changes as dch had changed the change log

echo "Here are the resulting files in $(pwd ..)"
echo "-----"
ls -l ../{*.changes,*.deb,*.buildinfo}
echo "-----"

# Let's try deploying
cd ..
([ ! -x deploy.sh ] || ./deploy.sh)
