package org.apache.spark.mllib.outlier

import breeze.linalg.sum
import com.quintor.VectorWithNormAndClass
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest._

/**
 * Created by Fokko on 24-6-15.
 */
class StocasticOutlierDetectionTest extends FlatSpec with Matchers with BeforeAndAfter {
  val appName = "OutlierDetector"
  val master = "local"
  val conf = new SparkConf().setAppName(appName).setMaster(master)
  val sc = new SparkContext(conf)

  "Computing the distance matrix " should "give symmetrical distances" in {

    val testdata = sc.parallelize(
      Seq(
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 3.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(5.0, 1.0)))
      ))

    val D = StocasticOutlierDetection.computeDistanceMatrix(testdata).collect().sortBy(dist => sum(dist))

    D(0).toArray should be(D(1).toArray)
  }

  "Computing the distance matrix " should "give the correct distances" in {

    val testdata = sc.parallelize(
      Seq(
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(2.0, 2.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(5.0, 1.0)))
      ))

    val D = StocasticOutlierDetection.computeDistanceMatrix(testdata).collect().sortBy(dist => sum(dist))

    D(0).toArray should be(Array(Math.sqrt(2.0), Math.sqrt(10.0)))
    D(1).toArray should be(Array(Math.sqrt(2.0), Math.sqrt(16.0)))
    D(2).toArray should be(Array(Math.sqrt(16.0), Math.sqrt(10.0)))
  }

  "Computing the affinity matrix " should "give the correct affinity" in {

    // The distance matrix
    val D = sc.parallelize(
      Seq(
        new DenseVector(Array(Math.sqrt(2.0), Math.sqrt(10.0))).toBreeze.toVector,
        new DenseVector(Array(Math.sqrt(2.0), Math.sqrt(16.0))).toBreeze.toVector,
        new DenseVector(Array(Math.sqrt(16.0), Math.sqrt(10.0))).toBreeze.toVector
      ))

    val perplexity = 1
    val tolerance = 0

    val A = StocasticOutlierDetection.computeAfinity(D, perplexity, tolerance).collect().sortBy(dist => sum(dist))

    System.out.println("Affinity matrix")
  }

  "Verifying the output of the SOS algorithm " should "assign the one true outlier" in {

    // The distance matrix

    val testdata = sc.parallelize(
      Seq(
        new VectorWithNormAndClass(1, new DenseVector(Array(1.0, 1.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(2.0, 2.0))),
        new VectorWithNormAndClass(1, new DenseVector(Array(5.0, 1.0))) // The outlier!
      ))


    val perplexity = 1
    val tolerance = 0

    val D = StocasticOutlierDetection.computeDistanceMatrix(testdata)
    val A = StocasticOutlierDetection.computeAfinity(D, perplexity, tolerance)
    val B = StocasticOutlierDetection.computeBindingProbabilities(A)
    val O = StocasticOutlierDetection.computeOutlierProbability(B)

    // Do a distributed sort, and then return to driver
    val output = O.sortBy(rank => rank * -1).collect

    Math.round(output(0)) should be 1L // The outlier!
    Math.round(output(1)) should be 0L
    Math.round(output(2)) should be 0L
  }

}
