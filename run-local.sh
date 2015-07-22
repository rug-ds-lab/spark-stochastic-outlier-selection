#!/bin/sh

docker-compose kill
docker-compose rm -f

a=8
n=2000

docker-compose scale master=1 zookeeper=1 kafka=$a worker=$a

docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh --create --zookeeper zookeeper:2181 --replication-factor 1 --partition $a --topic OutlierObservations$a

docker-compose run generator sbt "run $n $a OutlierObservations$a"

docker-compose run task /usr/spark/bin/spark-submit --class com.quintor.EvaluateOutlierDetectionDistributed --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar $n $a 4 OutlierObservations$a /tmp/results/memory.txt

docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh --zookeeper zookeeper:2181 --delete --topic OutlierObservations$a

