#!/bin/sh


a=1
b=10

while [ $b -lt 11 ]
do
	while [ $a -lt 21 ]
	do
	   docker-compose kill
	   docker-compose rm -f
	   docker-compose up -d master zookeeper kafka worker
	   docker-compose scale worker=$a
   
	   sec=`expr $a \* 5`
	   sleep $sec

	   docker-compose run task /usr/spark/bin/spark-submit --class com.quintor.EvaluateOutlierDetectionDistributed /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar 2000 /tmp/results/test$a.txt

	   a=`expr $a + 1`
	done
b=`expr $b + 1`
done
