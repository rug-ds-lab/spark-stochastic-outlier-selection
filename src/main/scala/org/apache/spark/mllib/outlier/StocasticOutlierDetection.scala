package org.apache.spark.mllib.outlier

import breeze.linalg
import breeze.linalg.{DenseVector, Vector, sum}
import com.quintor.VectorWithNormAndClass
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD

/**
 * Created by Fokko on 21-4-15.
 */
object StocasticOutlierDetection {

  implicit object compareByDistance extends Ordering[(Long, Double)] {
    override def compare(x: (Long, Double), y: (Long, Double)): Int = x._2 compare y._2
  }

  implicit def toBreeze(vector: org.apache.spark.mllib.linalg.Vector): breeze.linalg.Vector[Double] = vector.toBreeze
  implicit def fromBreeze(breezeVector: breeze.linalg.Vector[Double]): org.apache.spark.mllib.linalg.Vector = Vectors.fromBreeze(breezeVector)

  def computePerplexity(D: Vector[Double], beta: Double): (Double, Vector[Double]) = {
    val A = D.map(d => Math.exp(-d * beta))
    val sumA = sum(A)
    (Math.log(sumA) + beta * sum(D :* A) / sumA, A)
  }

  def computeAfinity(D: RDD[Vector[Double]], perplexity: Double, tolerance: Double): RDD[Vector[Double]] = {

    val logU = Math.log(perplexity)

    val A = D.map { r =>
      var beta = 1.0

      var betamin = Double.NegativeInfinity
      var betamax = Double.PositiveInfinity

      var iteration = 0

      val perplex = computePerplexity(r, perplexity)

      var Hdiff = perplex._1 - logU
      var thisA = perplex._2

      while (iteration < 500 && Math.abs(Hdiff) > tolerance) {
        beta = if(Hdiff > 0){
          betamin = beta
          if(betamax == Double.PositiveInfinity || betamax == Double.NegativeInfinity) {
            beta * 2.0
          } else {
            (beta + betamax) / 2.0
          }
        } else {
          betamax = beta
          if(betamin == Double.PositiveInfinity || betamin == Double.NegativeInfinity) {
            beta / 2.0
          } else {
            (beta+betamax) / 2.0
          }
        }
        val perplex = computePerplexity(r, beta)
        Hdiff = perplex._1 - logU
        thisA = perplex._2

        iteration += 1
      }

      System.out.println(iteration)

      thisA
    }

    A // Affinity Matrix
  }

  def computeBindingProbabilities(rows: RDD[linalg.Vector[Double]]) = rows.map( r => r :/ sum(r) )


  def computeDistanceMatrix(data: RDD[VectorWithNormAndClass]) = computeDistanceMatrixPair(data.zipWithUniqueId().map(_.swap))

  def computeDistanceMatrixPair(dataidx: RDD[(Long, VectorWithNormAndClass)]) = dataidx.cartesian(dataidx).flatMap {
      case (a: (Long, VectorWithNormAndClass), b: (Long, VectorWithNormAndClass)) =>
        if( a._1 != b._1 ) // Do not compute distance to self
          Some(a._1, Math.sqrt( Vectors.sqdist(a._2.vector, b._2.vector) ) ) // Math.sqrt(MLUtils.fastSquaredDistance(a._2.vector, a._2.norm, b._2.vector, b._2.norm))
        else
          None
    }.combineByKey(
        (v1) => List(v1),
        (c1: List[Double], v1: Double) => c1 :+ v1,
        (c1: List[Double], c2: List[Double]) => c1 ++ c2
      ).map {
      case (a, b) => new DenseVector(b.toArray[Double]).toVector
    }

  // Reduce the vector to one element by taking the product,
  // then sum the vector with length one to take the value...
  def computeOutlierProbability(rows: RDD[linalg.Vector[Double]]) = rows.map(v => sum(v.reduce((a,b) => a * (1-b))))

  def run(sc: SparkContext, data: RDD[VectorWithNormAndClass]) = {
    val n = data.count()

    // Perplexity cannot be larger than n-1
    val perplexity = Math.min(3, 30);

    val D = computeDistanceMatrix(data)

    val dCollected = D.collect()

    val tolerance = 1e-5

    System.out.println(Math.log(perplexity))

    val A = computeAfinity(D, perplexity, tolerance)

    val aCollected = A.collect()

    val B = computeBindingProbabilities(A)

    val bCollected = B.collect()

    val O = computeOutlierProbability(B)

    val oCollected = O.collect()

    // Do a distributed sort, and then return to driver
    O.sortBy(rank => rank * -1).collect
  }
}
