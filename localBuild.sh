#!/bin/sh

docker-compose -f docker-compose-build.yml kill
docker-compose -f docker-compose-build.yml rm -f
docker rmi -f sparkoutlierdetection_task
docker-compose -f docker-compose-build.yml up
