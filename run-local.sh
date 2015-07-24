#!/usr/bin/env bash

docker-compose kill
docker-compose rm -f

docker-compose up -d master zookeeper kafka worker

sleep 3

workers=1
n=500

while [ $workers -lt 11 ]
do
    b=1
    while [ $b -lt 6 ]
    do
        partitions=`expr $workers \* 3`

        docker-compose scale kafka=$workers worker=$workers

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
                    --create \
                    --zookeeper zookeeper:2181 \
                    --replication-factor 1 \
                    --partition $partitions \
                    --topic OutlierObservations$workers

        docker-compose run generator sbt "run $n $workers $partitions OutlierObservations$workers"

        docker-compose run task /usr/spark/bin/spark-submit \
                                    --class com.quintor.EvaluateOutlierDetectionDistributed \
                                    --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar \
                                    $n $workers $partitions OutlierObservations$workers /tmp/results/yay.txt

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
                        --zookeeper zookeeper:2181 \
                        --delete \
                        --topic OutlierObservations$workers

        b=`expr $b + 1`
    done
    n=`expr $n \* 2`
done