package com.quintor

import java.util.{Calendar, HashMap}

import breeze.linalg.DenseVector
import kafka.serializer.{DefaultDecoder, StringDecoder}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.util.Random

/**
 * Created by Fokko on 17-7-15.
 */
trait EvaluateOutlierDetection {
  val LS = System.getProperty("line.separator")
  val m = 10

  def sparkMaster: String

  def configKafka: HashMap[String, Object]

  def configSpark: Map[String, String]

  def nameApp: String

  def nameTopic: String = "OutlierObservations"

  def generateNormalVector: Array[Double] = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian).toArray
  }

  def populateKafka(n: Int): Unit = {
    val producer = new KafkaProducer[String, Array[Byte]](
      configKafka,
      new org.apache.kafka.common.serialization.StringSerializer,
      new ByteArraySerializer)
    (1 to n).foreach(pos => {
      System.out.println("Written: " + pos)
      producer.send(new ProducerRecord(nameTopic, generateNormalVector.pickle.value))
    })
    producer.close()
  }

  def performOutlierDetection(n: Int): Unit = {

    val conf = new SparkConf().setAppName(nameApp).setMaster(sparkMaster)
    val sc = new SparkContext(conf)

    val offsetRanges = Array[OffsetRange](
      OffsetRange.create(nameTopic, 0, 0, n)
    )

    val rdd = KafkaUtils.createRDD[String, Array[Byte], StringDecoder, DefaultDecoder](sc, configSpark, offsetRanges)

    // Start recording.
    val now = System.nanoTime

    val output = StocasticOutlierDetection.run(rdd.map(record => new DenseVector[Double](record._2.unpickle[Array[Double]]).toVector))
    val outcol = output.collect

    val micros = (System.nanoTime - now) / 1000

    val fw = new java.io.FileWriter("/tmp/test.txt", true)
    fw.write(Calendar.getInstance().getTime() + "," + outcol.length + "," + micros + LS + output.toDebugString + LS + LS + LS + LS)
    fw.close()
  }
}
