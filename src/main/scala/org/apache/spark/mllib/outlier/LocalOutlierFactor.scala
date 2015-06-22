package org.apache.spark.mllib.outlier

import com.quintor.VectorWithNormAndClass
import org.apache.spark.SparkContext
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD

/**
 * Created by Fokko on 21-4-15.
 */
object LocalOutlierFactor {
  val topN = 5


  implicit object compareByDistance extends Ordering[(Long, Double)] {
    override def compare(x: (Long, Double), y: (Long, Double)): Int = x._2 compare y._2
  }

  // Cantor pairing function
  //implicit def pair(a: (Long, Long)) = ((a._1 + a._2) * (a._1 + a._2 + 1)) / 2 + a._2;
  ////  val K = 10

  /*
  def extremeN [T](n: Int, li: List [T])
  (comp1: ((T, T) => Boolean), comp2: ((T, T) => Boolean)):
     List[T] = {

  def sortedIns (el: T, list: List[T]): List[T] =
    if (list.isEmpty) List (el) else
    if (comp2 (el, list.head)) el :: list else
      list.head :: sortedIns (el, list.tail)

  def updateSofar (sofar: List [T], el: T) : List [T] =
    if (comp1 (el, sofar.head))
      sortedIns (el, sofar.tail)
    else sofar

  (li.take (n) .sortWith (comp2 (_, _)) /: li.drop (n)) (updateSofar (_, _))
}

  //sort and trim a traversable (String, Long) tuple by _2 value of the tuple
  def topNs(xs: TraversableOnce[(Long, Double)]): List[(Long, Double)] = {
    var ss = List[(Long, Double)]()
    var min = Double.MaxValue
    var len = 0
    xs foreach { e =>
      if (len < topN || e._2 > min) {
        ss = (e :: ss).sortBy((f) => f._2)
        min = ss.head._2
        len += 1
      }
      if (len > topN) {
        ss = ss.tail
        min = ss.head._2
        len -= 1
      }
    }
    ss
  }
   */

  //sort and trim a traversable (String, Long) tuple by _2 value of the tuple
  def topNs(xs: TraversableOnce[(Long, Double)]): List[(Long, Double)] = {
    var ss = List[(Long, Double)]()
    var min = Double.MaxValue
    var len = 0
    xs foreach { e =>
      if (len < topN || e._2 > min) {
        ss = (e :: ss).sortBy((f) => f._2)
        min = ss.head._2
        len += 1
      }
      if (len > topN) {
        ss = ss.tail
        min = ss.head._2
        len -= 1
      }
    }
    ss
  }

  def run(sc: SparkContext, data: RDD[VectorWithNormAndClass]) = {
    val n = data.count()

    val dataidx = data.zipWithUniqueId().map(_.swap)

    /*
        // TODO Alles wordt nu dubbel uitgerekend, dit kan worden geoptimaliseerd, maar niet nu :)
        // val simss = dataidx.cartesian(dataidx).filter(a => a._1._2 < a._2._2).map {
        val simss = dataidx.cartesian(dataidx).filter(a => a._1._2 != a._2._2).map {
          case (a: (VectorWithNormAndClass, Long), b: (VectorWithNormAndClass, Long)) =>
            ((a._2, b._2), MLUtils.fastSquaredDistance(a._1.vector, a._1.norm, b._1.vector, b._1.norm))
        }.map { case ((i, j), sim) =>
          // The diagonal doesn't cary any information
          MatrixEntry(i, j,  Math.sqrt(sim))
        }


        val simss = dataidx.cartesian(dataidx).filter(a => a._1._2 != a._2._2).map {
          case (a: (VectorWithNormAndClass, Long), b: (VectorWithNormAndClass, Long)) =>
            ((a._2, b._2), MLUtils.fastSquaredDistance(a._1.vector, a._1.norm, b._1.vector, b._1.norm))
        }.map { case ((i, j), sim) =>
          // The diagonal doesn't cary any information
          MatrixEntry(i, j,  Math.sqrt(sim))
        }*/

    // Some edge-cases still need to be removed.
    // - do not compute the distance with itself
    // - k can be larger than k iff distance_(k) == distance_(k+1)

    // A naieve method
    val kClosest = dataidx.cartesian(dataidx).map {
      case (a: (Long, VectorWithNormAndClass), b: (Long, VectorWithNormAndClass)) =>
        (a._1, (b._1, MLUtils.fastSquaredDistance(a._2.vector, a._2.norm, b._2.vector, b._2.norm)))
    }.combineByKey(
      (v1) => List[(Long, Double)](v1),
      (c1: List[(Long, Double)], v1: (Long, Double)) => topNs(c1 :+ v1),
      (c1: List[(Long, Double)], c2: List[(Long, Double)]) => topNs(c1 ++ c2)
    )

      //.reduceByKey((a,b) => Math.max(a._2, b._2) )



    val dist = kClosest.flatMap {
      case (idxA: Long, nearest: List[(Long, Double)]) =>{
        val ldr = 1 / (nearest.reduceLeft((first: (Long, Double), second: (Long, Double)) => first._2 + second._2) / topN);
        nearest.map {
          case (idxB: Long, dist: Double) =>
            ((idxB, idxA), dist)
        }
      }
    }.cogroup{
      kClosest.flatMap {
        case (idxA: Long, nearest: List[(Long, Double)]) =>{
          nearest.map{
            case (idxB: Long, dist: Double) =>
              ((idxA, idxB), dist)
          }
        }
      }
    }


    val collected = dist.cogroup(ldr) {

    }

    /*
    val closest = dataidx.cartesian(dataidx).map {
      case (a: (Long, VectorWithNormAndClass), b: (Long, VectorWithNormAndClass)) =>
        (a._1, (b._1, MLUtils.fastSquaredDistance(a._2.vector, a._2.norm, b._2.vector, b._2.norm)))
    }.combineByKey(
      (v1) => List[(Long, Double)](v1),
      (c1: List[(Long, Double)], v1: (Long, Double)) => topNs(c1 :+ v1),
      (c1: List[(Long, Double)], c2: List[(Long, Double)]) => topNs(c1 ++ c2)
    ).reduce {
      case (a: Long, b: List[(Long, Double)]) =>
        (a, b, )
    }

    // LRB
    //


    val res = closest.collect

    System.out.println(res)


    System.out.println(closest.toDebugString)
*/
    1.0
  }
}
