import net.virtualvoid.sbt.graph.Plugin.graphSettings

val projectVersion = "0.1.8"

val projectSettings = Seq(
  description := "xx",
  organization := "io.soyl",
  version := projectVersion,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/soyl/soyl-akka-elect"))
)

val buildSettings = Seq(
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.6"),
  scalacOptions ++= Seq("-language:implicitConversions","-language:reflectiveCalls", "-feature", "-deprecation"),
  // fork (tests) to free resources. otherwise c* sessions are collected and will OOME at some point
  fork := true
)

val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <scm>
      <url>git@github.com:soyl/soyl-akka-elect.git</url>
      <connection>scm:git:git@github.com:soyl/soyl-akka-elect.git</connection>
    </scm>
    <developers>
      <developer>
        <id>algermissen</id>
        <name>Jan Algermissen</name>
        <url>https://github.com/algermissen</url>
      </developer>
    </developers>
  )
)

val Cassandra      = "2.1.2"
val CassandraDriver = "2.1.3"
val JodaTime       = "2.1"
val JodaConvertV    = "1.7"
val Logback        = "1.1.3"
val Scala          = "2.11.6"
val scalaTestV      = "2.2.4"
val scalazV         = "7.1.2"
val sparkV          = "1.4.1"
val SparkCassandra = "1.4.0-M3"
val cassandraUnitV = "2.1.3.1"
val KafkaClientsV  = "0.8.2.1"
val akkaV = "2.3.11"
val akkaStreamV = "1.0"
val akkaHttpV = "1.0"
val logstashLogbackV = "4.3"
val scalaLoggingV = "3.1.0"
val apacheCommonsCodecV = "1.10"
val mockitoV = "1.9.5"

val elastic4sV = "1.7.0"
val algebirdV = "0.11.0"



val akkaVersion = "2.3.7"

//  val pillar = "com.chrisomeara" %% "pillar" % "2.0.1" % sbt.IntegrationTest



lazy val ain = project.in(file("."))
  .settings(name := "soyl-akka-elect")
  .settings(projectSettings: _*)
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(graphSettings: _*)
  .settings(
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.4",
      "joda-time" % "joda-time" % "2.6",
      "org.joda" % "joda-convert" % "1.7",
      "org.quartz-scheduler" % "quartz" % "2.2.1",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.apache.cassandra" % "cassandra-all" % "2.1.3" % "test",
      //"com.chrisomeara" %% "pillar" % "2.0.1" % "test"
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    )
  )

// "org.slf4j" % "slf4j-api" % "1.7.9",
//"net.logstash.logback" % "logstash-logback-encoder" % "4.0"

