#!/bin/sh

docker-compose kill
docker-compose rm -f

docker-compose scale master=1 zookeeper=1 kafka=1 worker=1

a=8
n=500
c=1

while [ $a -lt 11 ]
do
    b=1
    while [ $b -lt 6 ]
    do
        docker-compose scale kafka=$a worker=$a

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh --create --replication-factor 1 --zookeeper zookeeper:2181 --partition $a --topic OutlierObservations$c

        docker-compose run generator sbt "run $n $a OutlierObservations$c"

        docker-compose run task /usr/spark/bin/spark-submit --class com.quintor.EvaluateOutlierDetectionDistributed --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar $n $a 6 OutlierObservations$c /tmp/results/scaling3.txt

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh --zookeeper zookeeper:2181 --delete --topic OutlierObservations$c

        b=`expr $b + 1`
	c=`expr $c + 1`
    done
    n=`expr $n \* 2`
done

