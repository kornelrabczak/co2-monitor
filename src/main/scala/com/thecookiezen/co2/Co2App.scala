package com.thecookiezen.co2

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import com.thecookiezen.co2.domain.{AlertLog, Statistics}
import com.thecookiezen.co2.sensor.Co2Sensor._
import com.thecookiezen.co2.sensor.SensorCoordinator
import com.thecookiezen.co2.sensor.SensorCoordinator.SensorRequest
import com.thecookiezen.co2.web.SensorAPI
import com.thecookiezen.co2.web.SensorAPI.SampleJson
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object Co2App extends App {

  type StoreMeasurement = (UUID, SampleJson) => Unit
  type GetStatus = UUID => Future[SensorState]
  type GetStatistics = UUID => Future[Statistics]
  type GetAlertLogs = UUID => Future[List[AlertLog]]

  val config = ConfigFactory.load("sensor.conf");

  val thresholdLevel = config.getInt("sensor.co2_threshold_level")
  val port = config.getInt("server.port")

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(1.seconds)

  val sensorCoordinator = system.actorOf(SensorCoordinator.props(thresholdLevel))

  val storeMeasurement: ActorRef => StoreMeasurement = coordinator => {
    case (uuid, sample) => coordinator ! SensorRequest(uuid, Co2SampleReading(sample.time, sample.co2))
  }

  val getStatus: ActorRef => GetStatus = coordinator => uuid => {
    coordinator ? SensorRequest(uuid, GetStatus)
  }.mapTo[SensorState]

  val getStatistics: ActorRef => GetStatistics = coordinator => uuid => {
    coordinator ? SensorRequest(uuid, GetStatistics)
  }.mapTo[Statistics]

  val getAlerts: ActorRef => GetAlertLogs = coordinator => uuid => {
    coordinator ? SensorRequest(uuid, GetAlertList)
  }.mapTo[List[AlertLog]]
  val bindingFuture = Http().newServerAt("localhost", port).bind(routes)

  def routes = new SensorAPI(
    storeMeasurement = storeMeasurement(sensorCoordinator),
    getStatus = getStatus(sensorCoordinator),
    getStatistics = getStatistics(sensorCoordinator),
    getAlerts = getAlerts(sensorCoordinator)
  ).routes

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
