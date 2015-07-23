name := "QuintorSparkOutlier"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.4.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.4.1",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.1",

  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",

  "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
)
