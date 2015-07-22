#!/bin/sh

sbt assembly

docker-compose kill
docker-compose rm -f
docker-compose up
