#!/bin/sh

a=1


while [ $a -lt 11 ]
do
   docker-compose kill
   docker-compose rm -f
   docker-compose up -d master zookeeper kafka worker
   docker-compose scale worker=$a
   
   sec=`expr $a \* 5`
   sleep $sec

   docker-compose run task

   a=`expr $a + 1`
done
