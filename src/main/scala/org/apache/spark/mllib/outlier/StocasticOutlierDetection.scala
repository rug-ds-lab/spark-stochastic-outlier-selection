package org.apache.spark.mllib.outlier

import breeze.linalg.functions.euclideanDistance
import breeze.linalg.{DenseVector, Vector, sum}
import org.apache.spark.rdd.RDD
import scala.language.implicitConversions

/**
 * Created by Fokko on 21-4-15.
 */
object StocasticOutlierDetection {
  val tolerance: Double = 0.0
  val maxIterations: Int = 50

  implicit def toBreeze(vector: org.apache.spark.mllib.linalg.Vector): breeze.linalg.Vector[Double] = vector.toBreeze
  implicit def fromBreeze(breezeVector: breeze.linalg.Vector[Double]): org.apache.spark.mllib.linalg.Vector = org.apache.spark.mllib.linalg.Vectors.fromBreeze(breezeVector)

  def binarySearch(affinity: Vector[Double],
                   logPerplexity: Double,
                   iteration: Int = 0,
                   beta: Double = 1.0,
                   betaMin: Double = Double.NegativeInfinity,
                   betaMax: Double = Double.PositiveInfinity): Vector[Double] = {

    val newAffinity = affinity.map(d => Math.exp(-d * beta))
    val sumA = sum(newAffinity)
    val hCurr = (Math.log(sumA) + beta * sum(affinity :* newAffinity) / sumA)
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

      binarySearch(affinity, logPerplexity, iteration + 1, search._1, search._2, search._3)
    }
    else
      newAffinity
  }

  def computeAfinity(dMatrix: RDD[(Long, Vector[Double])], perplexity: Double): RDD[(Long, Vector[Double])] = {
    val logPerplexity = Math.log(perplexity)
    dMatrix.map(r => (r._1, binarySearch(r._2, logPerplexity)))
  }

  def computeBindingProbabilities(rows: RDD[(Long, Vector[Double])]) = rows.map(r => (r._1, r._2 :/ sum(r._2)))

  def computeDistanceMatrix(data: RDD[Vector[Double]]) = computeDistanceMatrixPair(data.zipWithIndex.map(_.swap))

  def computeDistanceMatrixPair(data: RDD[(Long, Vector[Double])]): RDD[(Long, Vector[Double])] = data.cartesian(data).flatMap {
    case (a: (Long, Vector[Double]), b: (Long, Vector[Double])) =>
      if (a._1 != b._1) // Do not compute distance to self
        Some(a._1, euclideanDistance(a._2, b._2))
      else
        None
  }.combineByKey(
      (v1) => List(v1),
      (c1: List[Double], v1: Double) => c1 :+ v1,
      (c1: List[Double], c2: List[Double]) => c1 ++ c2
    ).map {
    case (a, b) => (a, new DenseVector(b.toArray[Double]).toVector)
  }

  def computeOutlierProbability(rows: RDD[(Long, Vector[Double])]): RDD[(Long, Double)] =
    rows.flatMap(r => r._2.toArray.zipWithIndex.map(p => (p._2 + (if (p._2 >= r._1) 1L else 0L), p._1))).foldByKey(1.0)((a, b) => a * (1.0 - b))


  def run(data: RDD[Vector[Double]]) = {
    // Perplexity cannot be larger than n-1
    val perplexity = Math.min(data.count() - 1, 30)

    val dMatrix = computeDistanceMatrix(data)
    val aMatrix = computeAfinity(dMatrix, perplexity)
    val bMatrix = computeBindingProbabilities(aMatrix)
    val oMatrix = computeOutlierProbability(bMatrix)

    // Do a distributed sort, and then collect to driver
    oMatrix.sortBy(rank => rank._2).collect
  }
}
