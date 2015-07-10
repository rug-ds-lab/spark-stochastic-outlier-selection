package com.quintor

import java.util.{Properties, UUID}

import breeze.linalg.DenseVector
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringDecoder
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionDistributed {
  //val brokers = "kafka:9092"

  val kafkaBrokers = System.getenv("ADDR_KAFKA")
  val sparkMaster = System.getenv("ADDR_SPARK")
  val topic = UUID.randomUUID().toString

  // Zookeeper connection properties
  val props = new Properties()

  props.put("producer.type", "sync")
  props.put("client.id", UUID.randomUUID().toString)
  props.put("metadata.broker.list", kafkaBrokers)
  props.put("serializer.class", "kafka.serializer.StringEncoder")

  val m = 10
  val n = 100000

  val appName = "OutlierDetector"

  def main(args: Array[String]) {
    System.out.println("Populating Kafka with test-data")
    populateKafka(n)
    System.out.println("Done")

    System.out.println("Applying outlier detection")
    performOutlierDetection
    System.out.println("Done")
  }

  def generateNormalVector: String = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian()).mkString(",")
  }

  def populateKafka(n: Int): Unit = {
    val producer = new Producer[String, String](new ProducerConfig(props))
    (1 to n).foreach(pos =>{
      producer.send(new KeyedMessage(topic, generateNormalVector))
      if(pos == (n/10)) {
        System.out.println("At " + pos + " of " + n)
      }
    })
  }

  def performOutlierDetection(): Unit = {

    val conf = new SparkConf().setAppName(appName).setMaster(sparkMaster)
    val sc = new SparkContext(conf)

    val now = System.nanoTime

    val offsetRanges = Array[OffsetRange](
      OffsetRange(topic, 0, 0, n)
    )

    val kafkaParams = Map("metadata.broker.list" -> kafkaBrokers)
    val rdd = KafkaUtils.createRDD[String, String, StringDecoder, StringDecoder](sc, kafkaParams, offsetRanges);

    // Map from string to Vector
    val dataset = rdd.map(record => new DenseVector[Double](record._2.split(',').map(_.toDouble)).toVector)

    val output = StocasticOutlierDetection.run(dataset)

    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))

    output.foreach(a => System.out.println(a._1 + ":" + a._2))
  }
}
