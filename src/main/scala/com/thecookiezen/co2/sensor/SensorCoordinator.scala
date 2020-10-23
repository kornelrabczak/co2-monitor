package com.thecookiezen.co2.sensor

import java.time.{LocalDate, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.thecookiezen.co2.domain.{AlertLog, Statistics}
import com.thecookiezen.co2.domain.Sensor._

import scala.concurrent.duration.DurationInt

object SensorCoordinator {
  implicit val timeout = Timeout(1.seconds)

  private val DefaultZoneId: ZoneId = ZoneId.of("UTC")

  def isDateFromLast30Days(time: ZonedDateTime): Boolean = {
    val thirtyDaysAgo = LocalDate.now(DefaultZoneId).minusDays(31)
    time.toLocalDate.isAfter(thirtyDaysAgo)
  }

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
