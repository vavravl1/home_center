name := """home_center"""
organization := "cz.home"

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

// DB
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "2.5.0",
  "org.scalikejdbc" %% "scalikejdbc-config" % "2.5.0",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.0",
  "org.scalikejdbc" %% "scalikejdbc-play-fixture" % "2.5.0",
  "org.scalikejdbc" %% "scalikejdbc-test" % "2.5.0" % "test",
  "com.h2database"  %  "h2" % "1.4.193",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  jdbc,
  cache,
  evolutions
)
//libraryDependencies += ws
libraryDependencies += filters

// Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % "2.5-M2"

// MQTT
resolvers += "Eclipse Paho Repo" at "https://repo.eclipse.org/content/repositories/paho-releases/"
libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0"

// Injector
libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.2.0" % "provided"
libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.2.0"

// Security
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "4.0.0-BETA4",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0-BETA4",
  "com.mohiva" %% "play-silhouette-persistence-memory" % "4.0.0-BETA4"
)

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")