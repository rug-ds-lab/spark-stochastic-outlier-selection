package com.quintor

import org.apache.spark.mllib.linalg.DenseVector
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

    val testdata = sc.parallelize(
      Seq(
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(2.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 2.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(2.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(8.0, 5.0)))
      ))

    val sos = StocasticOutlierDetection.run(sc, testdata)
    sos.map(println)

    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))
  }
}