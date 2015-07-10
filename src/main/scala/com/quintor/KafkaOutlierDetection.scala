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
object KafkaOutlierDetection {
  val brokers = "localhost:32885";
  val topic = "topic111c"

  // Zookeeper connection properties
  val props = new Properties()

  props.put("producer.type", "sync")
  props.put("client.id", UUID.randomUUID().toString)
  props.put("metadata.broker.list", brokers)
  props.put("serializer.class", "kafka.serializer.StringEncoder")

  val m = 10;
  val n = 100000;

  val appName = "OutlierDetector"
  val master = "local"

  def main(args: Array[String]) {
    populateKafka(n)
    performOutlierDetection
  }

  def generateNormalVector: String = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian()).mkString(",")
  }

  def populateKafka(n: Int): Unit = {
    val producer = new Producer[String, String](new ProducerConfig(props))
    (1 to n).foreach(_ => producer.send(new KeyedMessage(topic, generateNormalVector)))3
  }

  def performOutlierDetection(): Unit = {

    val conf = new SparkConf().setAppName(appName).setMaster(master)
    val sc = new SparkContext(conf)

    val now = System.nanoTime

    val offsetRanges = Array[OffsetRange](
      OffsetRange(topic, 0, 0, n)
    )

    val kafkaParams = Map("metadata.broker.list" -> brokers)
    val rdd = KafkaUtils.createRDD[String, String, StringDecoder, StringDecoder](sc, kafkaParams, offsetRanges);

    // Map from string to Vector
    val dataset = rdd.map(record => new DenseVector[Double](record._2.split(',').map(_.toDouble)).toVector)

    val output = StocasticOutlierDetection.run(dataset)

    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))

    output.foreach(a => System.out.println(a._1 + ":" + a._2))
  }
}
