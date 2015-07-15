package kafka.serializer

import scala.pickling.Defaults._
import scala.pickling.binary._

/**
 * Created by Fokko on 15-7-15.
 */
class ArrayDoubleEncoder extends Decoder[Array[Double]]   {
  override def fromBytes(bytes: Array[Byte]): Array[Double] = bytes.unpickle[Array[Double]]
}

class ArrayDoubleDecoder extends Encoder[Array[Double]] {
  override def toBytes(t: Array[Double]): Array[Byte] = t.pickle.value
}
