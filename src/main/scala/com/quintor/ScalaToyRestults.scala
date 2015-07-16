package com.quintor

import breeze.linalg.DenseVector
import org.apache.spark.mllib.outlier.StocasticOutlierDetection
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by Fokko on 16-7-15.
 */
object ScalaToyRestults {
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("OutlierDetector").setMaster("local")
    val sc = new SparkContext(conf)

    val rdd = sc.parallelize(Array(
      DenseVector(1.00, 1.00),
      DenseVector(3.00, 1.25),
      DenseVector(3.00, 3.00),
      DenseVector(1.00, 3.00),
      DenseVector(2.25, 2.25),
      DenseVector(8.00, 2.00)))

    val output = StocasticOutlierDetection.run(rdd.map(_.toVector), 4.5)
    val outcol = output.sortBy(_._2).collect

    outcol.foreach(System.out.println)
  }
}
