package com.quintor.data

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 * Created by fokko on 28-4-15.
 */
trait Dataset {
  def getData(sc: SparkContext): RDD[Vector[Double]]
  def numberOfClasses(sc:SparkContext): Int
}
