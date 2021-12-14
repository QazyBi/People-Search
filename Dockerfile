ARG OPENJDK_TAG=8u232
FROM openjdk:${OPENJDK_TAG}

ARG SBT_VERSION=1.4.9

# Install sbt
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion


#RUN mkdir -p /root/build/project
#ADD build.sbt /root/build/
#ADD ./project/plugins.sbt /root/build/project
#RUN cd /root/build && sbt compile


RUN mkdir -p /root/project
WORKDIR /root/project

COPY . /root/project/

EXPOSE 8080
#CMD bash
CMD ["sbt", "compile", "run"]
