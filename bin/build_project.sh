#!/bin/bash

version=${VERSION:-0.1.0-SNAPSHOT}
cwd=$(pwd)
server_src="sire-server/"
#cd sire-server
#
#cd ${cwd}
rm -rf target
mkdir target
#cp -r sire-server target/
cp -r sire-lib target/
#cd target/sire-server
#sbt stage
#cd ${cwd}
#ls target

docker build -t sire-server-core:${version} .