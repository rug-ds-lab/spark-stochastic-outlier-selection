name := "QuintorSparkOutlier"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "1.4.0"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "1.4.0"

libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.11" % "1.4.0"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.8.2.0"
