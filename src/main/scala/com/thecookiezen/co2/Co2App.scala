package com.thecookiezen.co2

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.thecookiezen.co2.sensor.SensorCoordinator
import com.thecookiezen.co2.web.SensorAPI
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Co2App {

  private val config: Config = ConfigFactory.load("sensor.conf");
  private val thresholdLevel = config.getInt("sensor.co2_threshold_level")
  private val port = config.getInt("server.port")

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private val sensorCoordinator = system.actorOf(SensorCoordinator.props(thresholdLevel))

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)

    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def routes: Route = new SensorAPI(
    storeMeasurement = SensorCoordinator.storeMeasurement(sensorCoordinator),
    getStatus = SensorCoordinator.getStatus(sensorCoordinator),
    getStatistics = SensorCoordinator.getStatistics(sensorCoordinator),
    getAlerts = SensorCoordinator.getAlerts(sensorCoordinator)
  ).routes
}
