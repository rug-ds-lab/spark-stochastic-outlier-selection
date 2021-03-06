package org.apache.spark.mllib.outlier

import breeze.linalg.{DenseVector, sum}
import breeze.numerics.{pow, sqrt}
import org.apache.spark.rdd.RDD

import scala.language.implicitConversions

/**
 * Created by Fokko on 21-4-15.
 */
object StocasticOutlierDetection {
  val DEFAULT_TOLERANCE: Double = 1e-20
  val DEFAULT_ITERATIONS: Int = 500
  val DEFAULT_PERPLEXITY = 30

  def binarySearch(affinity: DenseVector[Double],
                   logPerplexity: Double,
                   iteration: Int = 0,
                   beta: Double = 1.0,
                   betaMin: Double = Double.NegativeInfinity,
                   betaMax: Double = Double.PositiveInfinity,
                   maxIterations: Int = DEFAULT_ITERATIONS,
                   tolerance: Double = DEFAULT_TOLERANCE): DenseVector[Double] = {

    val newAffinity = affinity.map(d => Math.exp(-d * beta))
    val sumA = sum(newAffinity)
    val hCurr = Math.log(sumA) + beta * sum(affinity :* newAffinity) / sumA
    val hDiff = hCurr - logPerplexity

    if (iteration < maxIterations && Math.abs(hDiff) > tolerance) {
      val search = if (hDiff > 0)
        (if (betaMax == Double.PositiveInfinity || betaMax == Double.NegativeInfinity)
          beta * 2.0
        else
          (beta + betaMax) / 2.0, beta, betaMax)
      else
        (if (betaMin == Double.PositiveInfinity || betaMin == Double.NegativeInfinity)
          beta / 2.0
        else
          (beta + betaMin) / 2.0, betaMin, beta)

      binarySearch(affinity, logPerplexity, iteration + 1, search._1, search._2, search._3, maxIterations, tolerance)
    }
    else
      newAffinity
  }

  def computeAffinityMatrix(dMatrix: RDD[(Long, Array[Double])],
                            perplexity: Double = DEFAULT_PERPLEXITY): RDD[(Long, DenseVector[Double])] = {
    val logPerplexity = Math.log(perplexity)
    dMatrix.map(r => (r._1, binarySearch(new DenseVector(r._2), logPerplexity)))
  }

  def euclDistance(a: Array[Double], b: Array[Double]): Double = sqrt((a zip b).map { case (x, y) => pow(y - x, 2) }.sum)

  def computeBindingProbabilities(rows: RDD[(Long, DenseVector[Double])]): RDD[(Long, Array[Double])] =
    rows.map(r => (r._1, (r._2 :/ sum(r._2)).toArray))

  def computeDistanceMatrix(data: RDD[Array[Double]]): RDD[(Long, Array[Double])] = computeDistanceMatrixPair(data.zipWithUniqueId.map(_.swap))

  def computeDistanceMatrixPair(data: RDD[(Long, Array[Double])]): RDD[(Long, Array[Double])] =
    data.cartesian(data).flatMap {
      case (a: (Long, Array[Double]), b: (Long, Array[Double])) =>
        if (a._1 != b._1)
          Some(a._1, euclDistance(a._2, b._2))
        else
          None
    }.combineByKey(
        (v1) => List(v1),
        (c1: List[Double], v1: Double) => c1 :+ v1,
        (c1: List[Double], c2: List[Double]) => c1 ++ c2
      ).map {
      case (a, b) => (a, b.toArray)
    }

  def computeOutlierProbability(rows: RDD[(Long, Array[Double])]):
  RDD[(Long, Double)] =
    rows.flatMap(r => r._2.zipWithIndex.map(p =>
      (p._2 + (if (p._2 >= r._1) 1L else 0L), p._1))).foldByKey(1.0)((a, b) => a * (1.0 - b))
}
