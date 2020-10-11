package com.thecookiezen.co2.domain

import java.time.ZonedDateTime
import java.util.UUID

import com.thecookiezen.co2.domain.Co2Sample.Measurement

import scala.concurrent.Future

object Sensor {
  type StoreMeasurement = (UUID, Co2SampleReading) => Unit
  type GetStatus = UUID => Future[SensorState]
  type GetStatistics = UUID => Future[Statistics]
  type GetAlertLogs = UUID => Future[List[AlertLog]]


  sealed trait SensorState
  case object OK extends SensorState
  case object WARN extends SensorState
  case object ALERT extends SensorState


  sealed trait Command
  case class Co2SampleReading(time: ZonedDateTime, measurement: Measurement) extends Command
  case object GetStatus extends Command
  case object GetAlertList extends Command
  case object GetStatistics extends Command
}
