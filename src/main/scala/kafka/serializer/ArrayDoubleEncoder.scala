package kafka.serializer

import scala.pickling.Defaults._
import scala.pickling.binary._

/**
 * Created by Fokko on 15-7-15.
 */
class ArrayDoubleEncoder extends Encoder[Array[Double]] {
  override def toBytes(t: Array[Double]): Array[Byte] = t.pickle.value
}
