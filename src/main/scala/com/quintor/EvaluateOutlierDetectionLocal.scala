package com.quintor

import java.util

import org.apache.kafka.clients.producer.ProducerConfig

/**
 * Created by Fokko on 26-6-15.
 */
object EvaluateOutlierDetectionLocal extends EvaluateOutlierDetection {

  override def configKafka: util.HashMap[String, Object] = {
    val props = new util.HashMap[String, Object]()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dockerhost.summercamp.local:9092")

    props
  }

  override def configSpark: Map[String, String] = Map(
    "metadata.broker.list" -> "dockerhost.summercamp.local:9092"
  )

  override def sparkMaster: String = "spark://dockerhost.summercamp.local:7077"

  override def nameApp: String = this.getClass.getSimpleName

  def main(args: Array[String]) {
    val n = if (args.length > 0) {
      Integer.parseInt(args(0))
    } else {
      1000
    }

    System.out.println("Populating Kafka with test-data")
    populateKafka(n)
    System.out.println("Done")

    System.out.println("Applying outlier detection")
    performOutlierDetection(n)
    System.out.println("Done")

  }

}
