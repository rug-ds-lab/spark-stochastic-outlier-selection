package com.quintor

import java.util.{Calendar, HashMap}

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
    (1 to n).foreach(pos => producer.send(new ProducerRecord(nameTopic, generateNormalVector.pickle.value)))

    // Producer is not needed anymore, please close prevent leaking resources
    producer.close()
  }

  def performOutlierDetection(n: Int, filename: String = "/tmp/results/test.txt"): Unit = {

    val conf = new SparkConf().setAppName(nameApp).setMaster(sparkMaster)
    val sc = new SparkContext(conf)

    val offsetRanges = Array[OffsetRange](
      OffsetRange.create(nameTopic, 0, 0, n)
    )

    val rdd = KafkaUtils.createRDD[String, Array[Byte], StringDecoder, DefaultDecoder](sc, configSpark, offsetRanges)

    val finalPerplexity = 30

    // Start recording.
    val now = System.nanoTime

    val dMatrix = StocasticOutlierDetection.computeDistanceMatrix(rdd.map(record => record._2.unpickle[Array[Double]]))

    val step1 = (System.nanoTime - now) / 1000

    val aMatrix = StocasticOutlierDetection.computeAfinity(dMatrix, finalPerplexity)

    val step2 = (System.nanoTime - now) / 1000

    val bMatrix = StocasticOutlierDetection.computeBindingProbabilities(aMatrix)

    val step3 = (System.nanoTime - now) / 1000

    val oMatrix = StocasticOutlierDetection.computeOutlierProbability(bMatrix)

    val step4 = (System.nanoTime - now) / 1000

    val outcol = oMatrix.collect

    val fw = new java.io.FileWriter(filename, true)
    fw.write(outcol.length + "," + Calendar.getInstance().getTime() + "," + outcol.length + "," + step1 + "," + step2 + "," + step3 + "," + step4 + LS)
    fw.close()
  }
}
