import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val slf4j = Seq("org.slf4j" % "log4j-over-slf4j" % "1.7.30",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.30",
    "org.slf4j" % "jul-to-slf4j" % "1.7.30")

  lazy val tapirVersion = "0.17.0-M2"
  lazy val tapir = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion % Compile,
    "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion % Compile,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion % Compile,
    "io.circe" %% "circe-generic" % "0.13.0" % Compile,
    "io.circe" %% "circe-generic-extras" % "0.13.0" % Compile,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion % Compile,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion % Compile,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % tapirVersion % Compile
  )

  lazy val akkaVersion = "2.6.9"
  lazy val akkaHttpVersion = "10.2.0"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion % Compile,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion % Compile,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion % Compile,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Compile,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )
}
