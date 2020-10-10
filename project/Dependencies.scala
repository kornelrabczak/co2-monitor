import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val akkaVersion     = "2.6.8"
  lazy val akkaHttpVersion = "10.2.0"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion % Compile,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Compile,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )
}
