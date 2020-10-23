package com.thecookiezen.co2

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.thecookiezen.co2.sensor.SensorCoordinator.SensorRequest
import com.thecookiezen.co2.sensor.{Co2Sensor, SensorCoordinator}
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

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case SensorRequest(id, payload) => (id.toString, payload)
  }

  val numberOfShards = 10
  val extractShardId: ShardRegion.ExtractShardId = {
    case SensorRequest(id, _)       => (id.hashCode() % numberOfShards).toString
    case ShardRegion.StartEntity(id) => (id.toLong % numberOfShards).toString
  }

  private val sensorCoordinator: ActorRef = ClusterSharding(system).start(
    typeName = "Co2",
    entityProps = Co2Sensor.props(thresholdLevel),
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId
  )

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
