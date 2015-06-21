name := "QuintorSparkOutlier"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "1.3.1"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "1.3.1"

libraryDependencies += "amplab" % "spark-indexedrdd" % "0.1"

libraryDependencies += "com.invincea" % "spark-hash" % "0.1.2"
