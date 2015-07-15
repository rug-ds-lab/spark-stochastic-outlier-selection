package com.quintor

import java.util.{Calendar, Properties, UUID}

import breeze.linalg.DenseVector
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.{ArrayDoubleDecoder, StringDecoder}
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionDistributed {
  val LS = System.getProperty("line.separator")

  val kafkaBrokers = sys.env("ADDR_KAFKA")
  val sparkMaster = sys.env("ADDR_SPARK")
  val topic = UUID.randomUUID().toString

  // Zookeeper connection properties
  val props = new Properties()

  props.put("producer.type", "sync")
  props.put("client.id", UUID.randomUUID().toString)
  props.put("metadata.broker.list", kafkaBrokers)
  props.put("serializer.class", "kafka.serializer.ArrayDoubleEncoder")
  props.put("key.serializer.class", "kafka.serializer.StringEncoder")

  val m = 10

  val appName = "OutlierDetector"

  def main(args: Array[String]) {
    val n = Integer.parseInt(args(0))

    System.out.println("Populating Kafka with test-data")
    populateKafka(n)
    System.out.println("Done")

    // Wait 5 seconds to flush Kafka.
    Thread.sleep(5000);

    System.out.println("Applying outlier detection")
    performOutlierDetection(n)
    System.out.println("Done")
  }

  def generateNormalVector: Array[Double] = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian).toArray
  }

  def populateKafka(n: Int): Unit = {
    System.out.println("Connecting to kafka cluster: " + kafkaBrokers)
    val producer = new Producer[String, Array[Double]](new ProducerConfig(props))
    (1 to n).foreach(pos => {
      producer.send(new KeyedMessage(topic, generateNormalVector))
    })
  }

  def performOutlierDetection(n: Int): Unit = {

    val conf = new SparkConf().setAppName(appName).setMaster(sparkMaster)
    val sc = new SparkContext(conf)


    val offsetRanges = Array[OffsetRange](
      OffsetRange(topic, 0, 0, n)
    )

    val kafkaParams = Map("metadata.broker.list" -> kafkaBrokers)
    val rdd = KafkaUtils.createRDD[String, Array[Double], StringDecoder, ArrayDoubleDecoder](sc, kafkaParams, offsetRanges);

    // Start recording.
    val now = System.nanoTime

    val output = StocasticOutlierDetection.run(rdd.map(record => new DenseVector[Double](record._2).toVector))
    val outcol = output.collect

    val micros = (System.nanoTime - now) / 1000

    val fw = new java.io.FileWriter("/tmp/results/test.txt", true);
    fw.write(Calendar.getInstance().getTime() + "," + outcol.length + "," + micros + LS + output.toDebugString + LS + LS+ LS + LS )
    fw.close()
  }
}
