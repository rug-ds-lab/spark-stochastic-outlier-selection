package com.quintor

import java.util.Calendar

import kafka.serializer.{DefaultDecoder, StringDecoder}
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
    val brokers = Integer.parseInt(args(1))
    val partitions = Integer.parseInt(args(2))
    val nameTopic = args(3)
    val outputFile = args(4)

    val configSpark = Map("metadata.broker.list" -> (1 to brokers).map("kafka_" + _ + ":9092").reduceLeft((a, b) => a + "," + b))

    System.out.println("Applying outlier detection")

    val conf = new SparkConf().setMaster("spark://master:7077").setAppName("OutlierDetection-" + partitions + "-" + n)
    val sc = new SparkContext(conf)

    // Create the partitions
    val rest = n - (Math.floor(n.toDouble / partitions.toDouble) * partitions.toDouble)
    val offsetRanges = (0 to partitions).map(partition =>
      if (partition < rest)
        OffsetRange.create(nameTopic, partition, 0, Math.ceil(n.toDouble / partitions.toDouble).toLong)
      else
        OffsetRange.create(nameTopic, partition, 0, Math.floor(n.toDouble / partitions.toDouble).toLong)
    ).toArray

    val rdd = KafkaUtils.createRDD[String, Array[Byte], StringDecoder, DefaultDecoder](sc, configSpark, offsetRanges)
    val rddPersisted = rdd.map(record => record._2.unpickle[Array[Double]]).persist()

    System.out.println("Input partitions: " + rddPersisted.partitions.length)

    // Start recording
    val now = System.nanoTime

    val dMatrix = StocasticOutlierDetection.computeDistanceMatrix(rddPersisted)

    val step1 = System.nanoTime - now

    val aMatrix = StocasticOutlierDetection.computeAfinity(dMatrix)

    val step2 = System.nanoTime - now

    val bMatrix = StocasticOutlierDetection.computeBindingProbabilities(aMatrix)

    val step3 = System.nanoTime - now

    val oMatrix = StocasticOutlierDetection.computeOutlierProbability(bMatrix)

    val step4 = System.nanoTime - now

    val outcol = oMatrix.collect

    val fw = new java.io.FileWriter(outputFile, true)
    fw.write(Calendar.getInstance().getTime() + "," + partitions + "," + outcol.length + "," + step1 + "," + step2 + "," + step3 + "," + step4 + LS)
    fw.close()

    System.out.println("Done")
  }

}
