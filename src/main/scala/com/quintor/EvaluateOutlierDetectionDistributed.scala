package com.quintor

import java.util
import java.util.Calendar

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

  def generateNormalVector: Array[Double] = {
    val rnd = new Random()
    (1 to m).map(_ => rnd.nextGaussian).toArray
  }

  def main(args: Array[String]) {

    val n = Integer.parseInt(args(0))
    val partitions = Integer.parseInt(args(1))
    val nameTopic = args(2)
    val outputFile = args(3)

    val kafkaServer = (1 to partitions).map("kafka_" + _ + ":9092").reduceLeft((a, b) => a + "," + b);

    val configKafka = new util.HashMap[String, Object]()
    configKafka.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer)
    configKafka.put(ProducerConfig.ACKS_CONFIG, "all")

    val configSpark = Map("metadata.broker.list" -> kafkaServer)

    System.out.println("Populating Kafka: " + kafkaServer)

    val producer = new KafkaProducer[String, Array[Byte]](
      configKafka,
      new org.apache.kafka.common.serialization.StringSerializer,
      new ByteArraySerializer)

    (1 to (n+partitions)).foreach(pos => producer.send(new ProducerRecord(nameTopic, generateNormalVector.pickle.value)))

    // Producer is not needed anymore, please close prevent leaking resources
    producer.close()

    System.out.println("Done")

    // Wait 5 seconds to let Kafka flush.
    Thread.sleep(5000)

    System.out.println("Applying outlier detection")

    val conf = new SparkConf().setAppName("OutlierDetection").setMaster("spark://master:7077")
    val sc = new SparkContext(conf)

    // Create the partitions
    val rest = n - (Math.floor(n.toDouble / partitions.toDouble) * partitions.toDouble)
    val offsetRanges = (0 until partitions).map(partition =>
      if (partition < rest)
        OffsetRange.create(nameTopic, partition, 0, Math.ceil(n.toDouble / partitions.toDouble).toLong)
      else
        OffsetRange.create(nameTopic, partition, 0, Math.floor(n.toDouble / partitions.toDouble).toLong)
    ).toArray

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

    val fw = new java.io.FileWriter(outputFile, true)
    fw.write(Calendar.getInstance().getTime() + "," + partitions + "," + outcol.length + "," + step1 + "," + step2 + "," + step3 + "," + step4 + LS)
    fw.close()

    System.out.println("Done")
  }

}
