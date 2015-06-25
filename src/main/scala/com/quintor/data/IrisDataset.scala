package com.quintor.data

import breeze.linalg.DenseVector
import org.apache.spark.SparkContext

/**
 * Created by Fokko on 21-4-15.
 */
object IrisDataset  {
  def getData(sc: SparkContext) = sc.textFile("flower.csv").map { line =>
    val parts = line.split(',')
    new DenseVector(parts.map(_.toDouble)).toVector
  }

  def numberOfClasses(sc: SparkContext): Int = 2
}
