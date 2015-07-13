name := "QuintorSparkOutlier"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "1.4.0" % "provided",
  "org.apache.spark" % "spark-mllib_2.11" % "1.4.0" % "provided",
  "org.apache.spark" % "spark-streaming-kafka_2.11" % "1.4.0" % "provided",

  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",

  "org.apache.kafka" % "kafka-clients" % "0.8.2.1"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "pom.xml" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.first
}
