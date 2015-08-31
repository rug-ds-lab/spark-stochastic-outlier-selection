#!/bin/sh

docker-compose kill
docker-compose rm -f

docker-compose up -d master zookeeper kafka worker history

sleep 3

workers=4
n=100
c=1
part=12

b=1

rm -rf ./events
mkdir ./events
chmod -R 777 ./events

docker-compose scale kafka=$workers worker=$workers
sleep 10

echo "Creating topic with $part partitions"

docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
            --create \
            --zookeeper zookeeper:2181 \
            --replication-factor 1 \
            --partition $part \
            --topic OutlierObservations

docker-compose run generator sbt "run $n $workers $part OutlierObservations"

#while [ $b -lt 11 ]
#do
    docker-compose run task /usr/spark/bin/spark-submit \
                                --class com.quintor.EvaluateOutlierDetectionDistributed \
                                --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar \
                                $n $workers $part OutlierObservations /tmp/results/yay12.txt

    b=`expr $b + 1`
#done

