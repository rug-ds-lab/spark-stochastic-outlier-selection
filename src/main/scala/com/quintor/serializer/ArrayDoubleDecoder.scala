package com.quintor.serializer

import kafka.serializer.Decoder

import scala.pickling.Defaults._
import scala.pickling.binary._

/**
 * Created by Fokko on 15-7-15.
 */
class ArrayDoubleDecoder extends Decoder[Array[Double]] {
  override def fromBytes(bytes: Array[Byte]): Array[Double] = bytes.unpickle[Array[Double]]
}