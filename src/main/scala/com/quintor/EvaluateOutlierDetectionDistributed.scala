package com.quintor

import java.util.{Properties, UUID}

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionDistributed extends EvaluateOutlierDetection {
  override def configKafka: Properties = {
    val props = new Properties()

    props.put("client.id", UUID.randomUUID().toString)
    props.put("metadata.broker.list", sys.env("ADDR_KAFKA"))
    props.put("serializer.class", "com.quintor.serializer.ArrayDoubleEncoder")
    props.put("key.serializer.class", "kafka.serializer.StringEncoder")

    props
  }

  override def configSpark: Map[String, String] = Map(
    "metadata.broker.list" -> sys.env("ADDR_KAFKA")
  )

  override def sparkMaster: String = sys.env("ADDR_SPARK")

  override def nameTopic: String = UUID.randomUUID().toString

  override def nameApp: String = this.getClass.getSimpleName

  def main(args: Array[String]) {
    val n = Integer.parseInt(args(0))

    System.out.println("Populating Kafka with test-data")
    populateKafka(n)
    System.out.println("Done")

    // Wait 5 seconds to flush Kafka.
    Thread.sleep(5000)

    System.out.println("Applying outlier detection")
    performOutlierDetection(n)
    System.out.println("Done")
  }

}
