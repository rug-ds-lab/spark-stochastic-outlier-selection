package com.quintor

import java.util
import java.util.{Calendar, HashMap}

import kafka.serializer.{DefaultDecoder, StringDecoder}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.streaming.kafka.{KafkaUtils, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.util.Random

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionDistributed {

  val LS = System.getProperty("line.separator")
  val m = 10

  def nameTopic: String = "OutlierObservations"

  def generateNormalVector: Array[Double] = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian).toArray
  }

  def populateKafka(n: Int, configKafka: HashMap[String, Object]): Unit = {
    val producer = new KafkaProducer[String, Array[Byte]](
      configKafka,
      new org.apache.kafka.common.serialization.StringSerializer,
      new ByteArraySerializer)

    (1 to n).foreach(pos =>      producer.send(new ProducerRecord(nameTopic, generateNormalVector.pickle.value)))

    // Producer is not needed anymore, please close prevent leaking resources
    producer.close()
  }

  def performOutlierDetection(n: Int, paritions: Int, configSpark: Map[String, String], filename: String = "/tmp/results/test.txt"): Unit = {

    val conf = new SparkConf().setAppName("OutlierDetection").setMaster("spark://master:7077")
    val sc = new SparkContext(conf)

    // Create the paritions
    val offsetRanges = (0 until paritions).map(OffsetRange.create(nameTopic, _, 0, n)).toArray

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


  def main(args: Array[String]) {

    val partitions = Integer.parseInt(args(1));

    val n = Integer.parseInt(args(0))

    val kafkaServer = (1 to partitions).map("kafka_" + _ + ":9092").reduceLeft((a, b) => a + "," + b);

    val configKafka = new util.HashMap[String, Object]()
    configKafka.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)

    val configSpark = Map("metadata.broker.list" -> kafkaServer)

    System.out.println("Populating Kafka: " + kafkaServer)
    populateKafka(n, configKafka)
    System.out.println("Done")

    // Wait 5 seconds to let Kafka flush.
    Thread.sleep(5000)

    System.out.println("Applying outlier detection")
    performOutlierDetection(n, partitions, configSpark, args(2))
    System.out.println("Done")
  }

}
