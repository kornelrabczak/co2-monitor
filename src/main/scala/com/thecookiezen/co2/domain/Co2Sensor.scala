package com.thecookiezen.co2.domain

import java.time.LocalDate
import java.util.UUID

import com.thecookiezen.co2.domain.Co2Sensor.{AlertLog, SensorState}
import com.thecookiezen.co2.domain.Co2Sample.Measurement

case class Co2Sensor(id: UUID,
                     lastMeasurementTimestamp: LocalDate,
                     alertThreshold: Int,
                     measurements: List[Co2Sample],
                     currentState: SensorState,
                     logs: List[AlertLog])

object Co2Sensor {

  case class AlertLog(startTime: Long,
                   endTime: Long,
                   measurement1: Measurement,
                   measurement2: Measurement,
                   measurement3: Measurement)

  case class Statistics(average: Double, maxLevel: Int)

  sealed trait SensorState
  case object OK extends SensorState
  case object WARN extends SensorState
  case object ALERT extends SensorState

  case object GetStatus
  case object GetAlertList
  case object GetStatistics
}