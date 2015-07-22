package com.quintor.serializer

import java.util

import scala.pickling.Defaults._
import scala.pickling.binary._

import org.apache.kafka.common.serialization.Serializer

/**
 * Created by fokko on 21-7-15.
 */
class ArrayDoubleSerializer extends Serializer[Array[Double]] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def serialize(topic: String, data: Array[Double]): Array[Byte] = data.pickle.value

  override def close(): Unit = {

  }
}
