#!/bin/sh

docker-compose kill
docker-compose rm -f

docker-compose up -d master zookeeper kafka worker

sleep 3

workers=8
n=500
c=1

while [ $workers -lt 11 ]
do
    b=1
    while [ $b -lt 11 ]
    do
        part=`expr $workers \* 3`

	echo "Creating topic with $part partitions"

        docker-compose scale kafka=$workers worker=$workers

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
                    --create \
                    --zookeeper zookeeper:2181 \
                    --replication-factor 1 \
                    --partition $part \
                    --topic OutlierObservations$c


        docker-compose run generator sbt "run $n $workers $part OutlierObservations$c"

        docker-compose run task /usr/spark/bin/spark-submit \
                                    --class com.quintor.EvaluateOutlierDetectionDistributed \
                                    --master spark://master:7077 /tmp/app/target/scala-2.10/QuintorSparkOutlier-assembly-1.0.jar \
                                    $n $workers $part OutlierObservations$c /tmp/results/yay.txt

        docker-compose run kafka /opt/kafka_2.10-0.8.2.1/bin/kafka-topics.sh \
                        --zookeeper zookeeper:2181 \
                        --delete \
                        --topic OutlierObservations$c

        b=`expr $b + 1`
	c=`expr $c + 1`
    done
    n=`expr $n \* 2`
done

