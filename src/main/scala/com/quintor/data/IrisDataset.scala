package com.quintor.data

import com.quintor.VectorWithNormAndClass
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.DenseVector

/**
 * Created by Fokko on 21-4-15.
 */
object IrisDataset  {
  def getData(sc: SparkContext) = sc.textFile("flower.csv").map { line =>
    val parts = line.split(',')
    new VectorWithNormAndClass(0, new DenseVector(parts.map(_.toDouble)))
  }

  def numberOfClasses(sc: SparkContext): Int = 2
}
