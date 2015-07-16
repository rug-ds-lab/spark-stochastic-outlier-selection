package com.quintor

import java.util.{Calendar, Properties, UUID}

import breeze.linalg.DenseVector
import com.quintor.serializer.ArrayDoubleDecoder
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringDecoder
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionLocal {
  val LS = System.getProperty("line.separator")

  val kafkaBrokers = "localhost:9092"
  val sparkMaster = "local"
  val topic = UUID.randomUUID().toString

  // Zookeeper connection properties
  val props = new Properties()

  props.put("producer.type", "sync")
  props.put("client.id", UUID.randomUUID().toString)
  props.put("metadata.broker.list", kafkaBrokers)
  props.put("serializer.class", "com.quintor.serializer.ArrayDoubleEncoder")
  props.put("key.serializer.class", "kafka.serializer.StringEncoder")

  val m = 10

  val appName = "OutlierDetector"

  def main(args: Array[String]) {
    val n = if(args.length > 0) {
      Integer.parseInt(args(0))
    } else {
      1000
    }

    System.out.println("Populating Kafka with test-data")
    populateKafka(n)
    System.out.println("Done")

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
      OffsetRange.create(topic, 0, 0, n)
    )

    val kafkaParams = Map(
      "metadata.broker.list" -> kafkaBrokers
    )
    val rdd = KafkaUtils.createRDD[String, Array[Double], StringDecoder, ArrayDoubleDecoder](sc, kafkaParams, offsetRanges);

    // Start recording.
    val now = System.nanoTime

    val output = StocasticOutlierDetection.run(rdd.map(record => new DenseVector[Double](record._2).toVector))
    val outcol = output.collect

    val micros = (System.nanoTime - now) / 1000

    val fw = new java.io.FileWriter("/tmp/test.txt", true);
    fw.write(Calendar.getInstance().getTime() + "," + outcol.length + "," + micros + LS + output.toDebugString + LS + LS+ LS + LS )
    fw.close()
  }
}
