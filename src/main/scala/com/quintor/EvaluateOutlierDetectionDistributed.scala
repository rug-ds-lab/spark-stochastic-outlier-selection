package com.quintor

import java.util

import org.apache.kafka.clients.producer.ProducerConfig

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionDistributed extends EvaluateOutlierDetection {
  override def configKafka: util.HashMap[String, Object] = {
    val props = new util.HashMap[String, Object]()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sys.env("ADDR_KAFKA"))
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none")
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, "200");
    props.put(ProducerConfig.BLOCK_ON_BUFFER_FULL_CONFIG, "true")

    props
  }

  override def configSpark: Map[String, String] = Map(
    "metadata.broker.list" -> sys.env("ADDR_KAFKA")
  )

  override def sparkMaster: String = sys.env("ADDR_SPARK")

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
