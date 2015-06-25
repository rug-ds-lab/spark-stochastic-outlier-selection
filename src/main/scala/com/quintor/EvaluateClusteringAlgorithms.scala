package com.quintor

import breeze.linalg.DenseVector
import org.apache.spark.mllib.outlier._
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by Fokko on 21-4-15.
 */
object EvaluateClusteringAlgorithms {
  val appName = "OutlierDetector"
  val master = "local"

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName(appName).setMaster(master)
    val sc = new SparkContext(conf)

    val now = System.nanoTime

    val data = sc.parallelize(
      Seq(
        new DenseVector(Array(1.0, 1.0)).toVector,
        new DenseVector(Array(2.0, 1.0)).toVector,
        new DenseVector(Array(1.0, 2.0)).toVector,
        new DenseVector(Array(2.0, 2.0)).toVector,
        new DenseVector(Array(8.0, 5.0)).toVector
      ))

    val sos = StocasticOutlierDetection.run(data)

    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))
  }
}