package org.apache.spark.mllib.outlier

import breeze.linalg.{DenseVector, sum}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalactic.TolerantNumerics
import org.scalatest._

/**
 * Created by Fokko on 24-6-15.
 */
class StocasticOutlierDetectionTest extends FlatSpec with Matchers with BeforeAndAfter {
  val appName = "OutlierDetector"
  val master = "local"
  val conf = new SparkConf().setAppName(appName).setMaster(master)
  val sc = new SparkContext(conf)

  val perplexity = 1
  val tolerance = 0

  val epsilon = 1e-9f
  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)

  "Computing the distance matrix " should "give symmetrical distances" in {

    val data = sc.parallelize(
      Seq(
        new DenseVector(Array(1.0, 3.0)).toVector,
        new DenseVector(Array(5.0, 1.0)).toVector
      ))

    val D = StocasticOutlierDetection.computeDistanceMatrix(data).map(_._2).sortBy(dist => sum(dist)).collect()

    D(0).toArray should be(D(1).toArray)
  }

  "Computing the distance matrix " should "give the correct distances" in {

    val data = sc.parallelize(
      Seq(
        new DenseVector(Array(1.0, 1.0)).toVector,
        new DenseVector(Array(2.0, 2.0)).toVector,
        new DenseVector(Array(5.0, 1.0)).toVector
      ))

    val D = StocasticOutlierDetection.computeDistanceMatrix(data).map(_._2).sortBy(dist => sum(dist)).collect()

    D(0).toArray should be(Array(Math.sqrt(2.0), Math.sqrt(10.0)))
    D(1).toArray should be(Array(Math.sqrt(2.0), Math.sqrt(16.0)))
    D(2).toArray should be(Array(Math.sqrt(16.0), Math.sqrt(10.0)))
  }

  "Computing the perplexity of the vector " should "give the correct error" in {

    val vector = new DenseVector(Array(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 9.0, 10.0)).toVector

    val output = Array(
      3.67879441e-01,
      1.35335283e-01,
      4.97870684e-02,
      1.83156389e-02,
      6.73794700e-03,
      2.47875218e-03,
      3.35462628e-04,
      1.23409804e-04,
      4.53999298e-05
    )

    // Standard beta
    val beta = 1.0

    // even 1 iteratie
    val search = StocasticOutlierDetection.binarySearch(vector, Math.log(perplexity), 500, beta).toArray

    assert(search.length == output.length)
    search.zip(output).foreach(v => assert(v._1 === v._2))
  }

  "Computing the affinity matrix " should "give the correct affinity" in {

    // The distance matrix
    val D = sc.parallelize(
      Seq(
        (0L, new DenseVector(Array(Math.sqrt(2.0), Math.sqrt(10.0))).toVector),
        (1L, new DenseVector(Array(Math.sqrt(2.0), Math.sqrt(16.0))).toVector),
        (2L, new DenseVector(Array(Math.sqrt(16.0), Math.sqrt(10.0))).toVector)
      ))

    val A = StocasticOutlierDetection.computeAfinity(D, perplexity).map(_._2).sortBy(dist => sum(dist)).collect()

    assert(A(0)(0) === 6.61626106e-112)
    assert(A(0)(1) === 1.27343495e-088)

    assert(A(1)(0) === 2.21858114e-020)
    assert(A(1)(1) === 1.12846575e-044)

    assert(A(2)(0) === 1.48949023e-010)
    assert(A(2)(1) === 1.60381089e-028)
  }

  "Verify the binding probabilities " should "give the correct probabilities" in {

    // The distance matrix
    val D = sc.parallelize(
      Seq(
        (0L, new DenseVector(Array(6.61626106e-112, 1.27343495e-088)).toVector),
        (1L, new DenseVector(Array(2.21858114e-020, 1.12846575e-044)).toVector),
        (2L, new DenseVector(Array(1.48949023e-010, 1.60381089e-028)).toVector)
      ))

    val B = StocasticOutlierDetection.computeBindingProbabilities(D).map(_._2).sortBy(dist => sum(dist)).collect()

    assert(B(0)(0) === 5.19560192e-24)
    assert(B(0)(1) === 1.00000000e+00)

    assert(B(1)(0) === 1.00000000e+00)
    assert(B(1)(1) === 5.08642993e-25)

    assert(B(2)(0) === 1.00000000e+00)
    assert(B(2)(1) === 1.07675154e-18)
  }

  "Verifying the product " should "should provide valid products" in {

    val data = sc.parallelize(
      Seq(
        (0L, new DenseVector(Array(0.5, 0.3)).toVector),
        (1L, new DenseVector(Array(0.25, 0.1)).toVector),
        (2L, new DenseVector(Array(0.8, 0.8)).toVector)
      ))

    val O = StocasticOutlierDetection.computeOutlierProbability(data).map(_._2).sortBy(dist => dist).collect()

    val out0 = ((1.0 - 0.5) * (1.0 - 0.0) * (1.0 - 0.8));
    val out1 = ((1.0 - 0.0) * (1.0 - 0.25) * (1.0 - 0.8));
    val out2 = ((1.0 - 0.3) * (1.0 - 0.1) * (1.0 - 0));

    assert(O.length == 3)

    assert(O(0) === out0)
    assert(O(1) === out1)
    assert(O(2) === out2)
  }

  "Verifying the output of the SOS algorithm " should "assign the one true outlier" in {

    // The distance matrix
    val data = sc.parallelize(
      Seq(
        new DenseVector(Array(1.0, 1.0)).toVector,
        new DenseVector(Array(1.0, 2.0)).toVector,
        new DenseVector(Array(2.0, 1.0)).toVector,
        new DenseVector(Array(2.0, 2.0)).toVector,
        new DenseVector(Array(5.0, 8.0)).toVector // The outlier!
      ))

    // Parameters
    val perplexity = 3

    // Process the steps of the algorithm
    val D = StocasticOutlierDetection.computeDistanceMatrix(data)
    val A = StocasticOutlierDetection.computeAfinity(D, perplexity)
    val B = StocasticOutlierDetection.computeBindingProbabilities(A)

    val O = StocasticOutlierDetection.computeOutlierProbability(B)

    // Do a distributed sort, and then return to driver
    val output = O.map(_._2).sortBy(rank => rank).collect

    assert(output.length == 5)
    assert(output(0) === 0.11240777825013754943)
    assert(output(1) === 0.23265631826249269509)
    assert(output(2) === 0.27136924729480282892)
    assert(output(3) === 0.28980746572456678178)
    assert(output(4) === 0.94237591774211593165) // The outlier!
  }

}
