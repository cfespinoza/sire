#!/usr/bin/bash

docker tag sire-server:latest registry.heroku.com/sire-server/web:latest
docker push registry.heroku.com/sire-server/web:latest
~/Descargas/heroku/bin/heroku container:release web -a sire-server