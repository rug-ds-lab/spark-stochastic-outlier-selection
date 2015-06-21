package com.quintor.data

import com.quintor.VectorWithNormAndClass
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.DenseVector

/**
 * Created by Fokko on 21-4-15.
 */
object IrisDataset extends Dataset {
  override def getData(sc: SparkContext) = sc.textFile("flower.csv").map { line =>
    val parts = line.split(',')
    new VectorWithNormAndClass(parts.head.toInt, new DenseVector(parts.tail.map(_.toDouble)))
  }

  override def numberOfClasses(sc: SparkContext): Int = 2
}
