package com.thecookiezen.co2.sensor

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.ask
import akka.util.Timeout
import com.thecookiezen.co2.domain.{AlertLog, Statistics}
import com.thecookiezen.co2.domain.Sensor._

import scala.concurrent.duration.DurationInt

object SensorCoordinator {
  implicit val timeout = Timeout(1.seconds)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case SensorRequest(id, payload) => (id.toString, payload)
  }

  val numberOfShards = 10
  val extractShardId: ShardRegion.ExtractShardId = {
    case SensorRequest(id, _)       => (id.hashCode() % numberOfShards).toString
    case ShardRegion.StartEntity(id) => (id.toLong % numberOfShards).toString
  }

  def createCoordinator(system: ActorSystem, thresholdLevel: Int): ActorRef = ClusterSharding(system).start(
    typeName = "Co2",
    entityProps = Co2Sensor.props(thresholdLevel),
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId
  )

  case class SensorRequest(id: UUID, command: Command)

  val storeMeasurement: ActorRef => StoreMeasurement = coordinator => {
    case (uuid, sample) => coordinator ! SensorRequest(uuid, sample)
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
}
