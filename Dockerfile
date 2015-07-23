FROM java:8

MAINTAINER Fokko Driesprong <fokko@driesprong.frl>

RUN echo "deb http://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key update
RUN apt-get update && apt-get -y --force-yes install sbt

ADD . /tmp/app
WORKDIR /tmp/app

RUN sbt compile

CMD sbt "run 1000 1 3 OutlierObservations /tmp/results/yay.txt"