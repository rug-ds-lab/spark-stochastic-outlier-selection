package com.quintor

import org.apache.spark.mllib.linalg._


/**
 * Created by Fokko on 22-6-15.
 */

class VectorWithNormAndClass(val targetClass: Int, val vector: DenseVector, val norm: Double) extends Serializable {

  def this(targetClass: Int, vector: DenseVector) = this(targetClass, vector, Vectors.norm(vector, 2.0))

  /** Converts the vector to a dense vector. */
  //def toDense: VectorWithNormAndClass = new VectorWithNormAndClass(targetClass, Vectors.dense(vector.toArray), norm)
}
