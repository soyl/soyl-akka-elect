import net.virtualvoid.sbt.graph.Plugin.graphSettings

val projectVersion = "0.2.0"

val projectSettings = Seq(
  description := "Runtime leader election with Akka using Cassandra",
  organization := "io.soyl",
  version := projectVersion,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/soyl/soyl-akka-elect"))
)

val buildSettings = Seq(
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),
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

lazy val ain = project.in(file("."))
  .settings(name := "soyl-akka-elect")
  .settings(projectSettings: _*)
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(graphSettings: _*)
  .settings(
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0-rc1",
      "joda-time" % "joda-time" % "2.6",
      "org.joda" % "joda-convert" % "1.7",
      "org.quartz-scheduler" % "quartz" % "2.2.1",
      "com.typesafe.akka" %% "akka-actor" % "2.4.1",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.1" % "test",
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      "org.apache.cassandra" % "cassandra-all" % "2.1.3" % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    )
  )
