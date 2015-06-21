package org.apache.spark.mllib.linalg

/**
 * Created by Fokko on 17-4-15.
 */
object VectorPub {

  implicit class VectorPublications(val vector : Vector) extends AnyVal {
    def toBreeze : breeze.linalg.Vector[scala.Double] = vector.toBreeze
  }

  implicit class BreezeVectorPublications(val breezeVector : breeze.linalg.Vector[Double]) extends AnyVal {
    def fromBreeze : Vector = Vectors.fromBreeze(breezeVector)
  }
}
