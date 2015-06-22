package org.apache.spark.mllib.outlier

import com.quintor.VectorWithNormAndClass
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.{IndexedRowMatrix, IndexedRow}
import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD

/**
 * Created by Fokko on 21-4-15.
 */
object StocasticOutlierDetection {

  implicit object compareByDistance extends Ordering[(Long, Double)] {
    override def compare(x: (Long, Double), y: (Long, Double)): Int = x._2 compare y._2
  }


  implicit class VectorPublications(val vector: Vector) extends AnyVal {
    def toBreeze: breeze.linalg.Vector[scala.Double] = vector.toBreeze
  }

  implicit class BreezeVectorPublications(val breezeVector: breeze.linalg.Vector[Double]) extends AnyVal {
    def fromBreeze: Vector = Vectors.fromBreeze(breezeVector)
  }

  def computePerplexity(D: List[Double], beta: Double): Unit = {
    val A = D.map { d =>
      Math.exp(-d * beta)
    }

    val Asum = A.sum
    val ad = Math.log(aSum) + beta * (D * A) / aSum;


  }

  def run(sc: SparkContext, data: RDD[VectorWithNormAndClass]) = {
    val n = data.count()
    val dataidx = data.zipWithUniqueId().map(_.swap)

    // Compute distance matrix
    val vectorDistances = dataidx.cartesian(dataidx).map {
      case (a: (Long, VectorWithNormAndClass), b: (Long, VectorWithNormAndClass)) =>
        (a._1, MLUtils.fastSquaredDistance(a._2.vector, a._2.norm, b._2.vector, b._2.norm))
    }.combineByKey(
        (v1) => List(v1),
        (c1: List[Double], v1: Double) => c1 :+ v1,
        (c1: List[Double], c2: List[Double]) => c1 ++ c2
      ).map {
      case (a, b) => new IndexedRow(a, new DenseVector(b.toArray))
    }

    val valDistanceMatrix = new IndexedRowMatrix(vectorDistances)




    
    1.0
  }
}
