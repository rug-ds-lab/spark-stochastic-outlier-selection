#!/bin/sh

b=1

sbt assembly

while [ $b -lt 11 ]
do
	docker-compose kill
	docker-compose rm -f
	docker-compose up
    b=`expr $b + 1`
done
