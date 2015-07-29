#!/bin/sh

docker-compose kill
docker-compose rm -f

docker-compose up -d master zookeeper kafka worker

sleep 3

workers=1
n=2000

while [ $workers -lt 21 ]
do
    b=1

    docker-compose scale kafka=$workers worker=$workers
    sleep 10
    part=`expr $workers \* 3`

    echo "Creating topic with $part partitions"

    docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
                --create \
                --zookeeper zookeeper:2181 \
                --replication-factor 1 \
                --partition $part \
                --topic OutlierObservations

    docker-compose run generator sbt "run $n $workers $part OutlierObservations"

    while [ $b -lt 6 ]
    do
        docker-compose run task /usr/spark/bin/spark-submit \
                                    --class com.quintor.EvaluateOutlierDetectionDistributed \
                                    --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar \
                                    $n $workers $part OutlierObservations /tmp/results/yayssss.txt

        b=`expr $b + 1`
    done

    docker-compose kill
    docker-compose rm -f

    docker-compose up -d master zookeeper kafka worker
    sleep 10

    workers=`expr $workers + 1`
done

