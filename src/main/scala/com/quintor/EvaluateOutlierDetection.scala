package com.quintor

import java.util.{Calendar, Properties}

import breeze.linalg.DenseVector
import com.quintor.serializer.ArrayDoubleDecoder
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringDecoder
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

/**
 * Created by Fokko on 17-7-15.
 */
trait EvaluateOutlierDetection {
  val LS = System.getProperty("line.separator")

  val m = 10

  def configKafka: Properties

  def configSpark: Map[String, String]

  def nameApp: String

  def nameTopic: String

  def generateNormalVector: Array[Double] = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian).toArray
  }

  def populateKafka(n: Int): Unit = {
    val producer = new Producer[String, Array[Double]](new ProducerConfig(configKafka))
    (1 to n).foreach(pos => {
      producer.send(new KeyedMessage(nameTopic, generateNormalVector))
    })
  }

  def performOutlierDetection(n: Int): Unit = {

    val conf = new SparkConf().setAppName(nameApp)
    val sc = new SparkContext(conf)

    val offsetRanges = Array[OffsetRange](
      OffsetRange.create(nameTopic, 0, 0, n)
    )

    val rdd = KafkaUtils.createRDD[String, Array[Double], StringDecoder, ArrayDoubleDecoder](sc, configSpark, offsetRanges);

    // Start recording.
    val now = System.nanoTime

    val output = StocasticOutlierDetection.run(rdd.map(record => new DenseVector[Double](record._2).toVector))
    val outcol = output.collect

    val micros = (System.nanoTime - now) / 1000

    val fw = new java.io.FileWriter("/tmp/test.txt", true);
    fw.write(Calendar.getInstance().getTime() + "," + outcol.length + "," + micros + LS + output.toDebugString + LS + LS + LS + LS)
    fw.close()
  }
}
