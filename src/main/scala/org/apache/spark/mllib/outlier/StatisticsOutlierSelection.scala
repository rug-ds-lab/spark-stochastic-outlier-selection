package org.apache.spark.mllib.outlier

/**
 * Created by fokko on 28-4-15.
 */
object StatisticsOutlierSelection {


  /*
val tolerance = 1e-5

def run(sc: SparkContext, data: Seq[DenseVector]) {


  val distanceVectors = data.map {
    case (v1, k1) => ( new DenseVector(data.map {
      case (v2, k2) => if (k1 != k2) sqrt((v1 - v2).foldLeft(0.0)((a, b) => a + pow(b, 2))) else 0
    }), k1)
  }

  val affinityVectors = distanceVectors.map {
    case (row, idx) => (optimize(row, idx.toInt), idx)
  }
  val affinityVectors = distanceVectors.map(x => {


    val beta = DenseVector.fill(n){1.0}

    for(i <- 1 to 50) {

    }

    x.map(D => D match {
      case 0 => 0
      case _ => exp(-D * 1)
    })
  })

val bindingVectors = affinityVectors.map {
    case (row, idx) => (row :/= sum(row), idx)
  }

  val outlierr = bindingVectors.map {
    case (row, idx) => {
      row.update(idx.toInt, 1) // Diagonaal op 1 zetten zodat die niks doet :)
      row.reduceLeft((a, b) => {
        val c = a * b
        c
      })
    }
  }

  outlierr.map { outlierValue => System.out.println(outlierValue) }
  System.out.println(outlierr);
}

val perplexity = 30;
val perplexityLog = log(30);

def optimize(D: DenseVector[Double],
             diagonal: Integer,
             beta: Double = 1,
             betamin: Double = Double.NegativeInfinity,
             betamax: Double = Double.PositiveInfinity,
             itr: Integer = 50): DenseVector[Double] = {

  // Always set the diagonal position to zero.
  D.update(diagonal, 0);

  if (itr <= 0)
    return D

  val A = exp(-D * beta)
  val sumA = sum(A)
  val H = (log(sumA) + beta) * (sum(D :* A) / sumA)

  val Hdiff = H - perplexityLog

  val newBeta = if (Hdiff > 0) {
    if (betamax == Double.PositiveInfinity || betamax == Double.NegativeInfinity)
      beta / 2;
    else
      (beta + betamax) / 2
  }
  else if (Hdiff < 0) {
    if (betamin == Double.PositiveInfinity || betamin == Double.NegativeInfinity)
      beta / 2;
    else
      (beta + betamin) / 2
  }
  else beta

  optimize(D, diagonal, newBeta, min(beta,betamin), max(beta,betamax), itr - 1)

  }
*/
}
