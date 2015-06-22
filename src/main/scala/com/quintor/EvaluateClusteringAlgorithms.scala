package com.quintor

import com.quintor.data.IrisDataset
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.spark.mllib.outlier._

/**
 * Created by fokko on 21-4-15.
 */
object EvaluateClusteringAlgorithms {

  def main(args: Array[String]): Unit = {

    val appName = "OutlierDetector"
    val master = "local"

    val conf = new SparkConf().setAppName(appName).setMaster(master)

    val sc = new SparkContext(conf)

    val data = IrisDataset.getData(sc)

    val normaly = data.filter(_.targetClass == 3);

    val now = System.nanoTime

    val testdata = sc.parallelize(
      Seq(
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(5.0, 5.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(3.0, 3.0)))
      ))

    val normality = LocalOutlierFactor.run(sc, normaly)

    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))
  }
}