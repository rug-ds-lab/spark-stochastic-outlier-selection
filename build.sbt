name := "QuintorSparkOutlier"

version := "1.0"

scalaVersion := "2.10.5"

resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.4.0",
  "org.apache.spark" % "spark-mllib_2.10" % "1.4.0",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.0",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",

  "org.apache.kafka" % "kafka_2.10" % "0.8.2.1",

  "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
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
      case "license" :: xs =>
        MergeStrategy.discard
      case "license.txt" :: xs =>
        MergeStrategy.discard
      case "notice" :: xs =>
        MergeStrategy.discard
      case "notice.txt" :: xs =>
        MergeStrategy.discard
      case "pom.properties" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}
