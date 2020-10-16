package com.thecookiezen.co2.domain

import java.util.UUID

import com.thecookiezen.co2.domain.SensorProtocol.Co2SampleReading

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
}
