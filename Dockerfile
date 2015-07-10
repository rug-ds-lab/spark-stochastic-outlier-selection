FROM gettyimages/spark

MAINTAINER Fokko Driesprong <fokko@driesprong.frl>

ENV SBT_VERSION  0.13.8
ENV SBT_JAR      https://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/$SBT_VERSION/sbt-launch.jar

ADD  $SBT_JAR  /usr/local/bin/sbt-launch.jar
COPY sbt.sh    /usr/local/bin/sbt

RUN sbt

RUN sbt clean package

ADD target/scala-2.11/quintorsparkoutlier_2.11-1.0.jar /tmp/task.jar

CMD "/usr/spark/bin/spark-submit --class com.quintor.EvaluateOutlierDetectionDistributed --master spark://master:7077 /tmp/task.jar