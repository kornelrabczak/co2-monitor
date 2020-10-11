package com.thecookiezen.co2

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.thecookiezen.co2.sensor.SensorCoordinator
import com.thecookiezen.co2.web.SensorAPI
import com.typesafe.config.ConfigFactory

import scala.io.StdIn

object Co2App extends App {

  val config = ConfigFactory.load("sensor.conf");

  val thresholdLevel = config.getInt("sensor.co2_threshold_level")
  val port = config.getInt("server.port")

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val sensorCoordinator = system.actorOf(SensorCoordinator.props(thresholdLevel))

  val bindingFuture = Http().newServerAt("localhost", port).bind(routes)

  def routes = new SensorAPI(
    storeMeasurement = SensorCoordinator.storeMeasurement(sensorCoordinator),
    getStatus = SensorCoordinator.getStatus(sensorCoordinator),
    getStatistics = SensorCoordinator.getStatistics(sensorCoordinator),
    getAlerts = SensorCoordinator.getAlerts(sensorCoordinator)
  ).routes

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
