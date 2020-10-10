package com.thecookiezen.co2.domain

import java.time._
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.domain.Co2Sensor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

class Co2Sensor(id: UUID, alertThreshold: Measurement) extends Actor with ActorLogging {

  private val alertLogs: ListBuffer[AlertLog] = ListBuffer.empty
  private var measurements: ListBuffer[Co2Sample] = ListBuffer.empty

  log.info("Device actor {} started", id)

  override def receive: Receive = oKwithData.orElse(handleQueries)

  private def oKwithData: Receive = {
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      measurements += sample
      if (sample.sample > alertThreshold) {
        val alert = AlertLog(startTime = sample.utcTimestamp, measurements = List(sample.sample))
        changeState(warning(okCounter = 0, alertCounter = 1, possibleAlertLog = alert))
      }
    case GetStatus => sender() ! OK
  }

  private def warning(okCounter: Int, alertCounter: Int, possibleAlertLog: AlertLog): Receive = {
    case GetStatus => sender() ! WARN
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      measurements += sample

      if (sample.sample > alertThreshold) {
        val alertLog = possibleAlertLog.copy(measurements = possibleAlertLog.measurements :+ sample.sample)
        if (alertCounter + 1 > 2) {
          changeState(alert(0, alertLog))
        } else {
          changeState(warning(0, alertCounter + 1, alertLog))
        }
      } else {
        if (okCounter + 1 > 2) {
          changeState(oKwithData)
        } else {
          changeState(warning(okCounter + 1, 0, possibleAlertLog.copy(measurements = List.empty)))
        }
      }
  }

  private def alert(okCounter: Int, alertLog: AlertLog): Receive = {
    case GetStatus => sender() ! ALERT
    case GetAlertList => sender() ! alertLogs.toList :+ alertLog
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      measurements += sample

      if (sample.sample > alertThreshold) {
        changeState(alert(0, alertLog))
      } else {
        if (okCounter + 1 > 2) {
          alertLogs += alertLog.copy(endTime = Some(sample.utcTimestamp))
          changeState(oKwithData)
        } else {
          changeState(alert(okCounter + 1, alertLog))
        }
      }
  }

  private def handleQueries: Receive = {
    case GetAlertList => sender() ! alertLogs.toList
    case GetStatistics => (Co2Sample.average(measurements.toList), Co2Sample.max(measurements.toList)) match {
      case (Some(avg), Some(max)) => sender() ! Statistics(avg, max.sample)
      case _ => sender() ! Statistics.empty
    }
    case CleanOldSamples(duration) =>
      val keepDays = duration.toDays
      measurements = measurements.filter { sample =>
        val fromDate = LocalDate.now(DefaulZoneId).minusDays(keepDays + 1)
        val sampleDate = LocalDate.ofInstant(Instant.ofEpochSecond(sample.utcTimestamp), DefaulZoneId)
        sampleDate.isAfter(fromDate)
      }
  }

  private def changeState(to: Receive): Unit = {
    context.become(to.orElse(handleQueries))
  }
}

object Co2Sensor {
  private val DefaulZoneId: ZoneId = ZoneId.of("UTC");

  def props(id: UUID, alertThreshold: Measurement): Props = Props(new Co2Sensor(id, alertThreshold))

  def isDateFromLast30Days(time: LocalDateTime): Boolean = {
    val thirtyDaysAgo = LocalDate.now(DefaulZoneId).minusDays(31)
    time.atZone(DefaulZoneId).toLocalDate.isAfter(thirtyDaysAgo)
  }

  case class Co2SampleReading(time: LocalDateTime, measurement: Measurement)

  case class CleanOldSamples(duration: Duration)

  sealed trait SensorState
  case object OK extends SensorState
  case object WARN extends SensorState
  case object ALERT extends SensorState

  case object GetStatus
  case object GetAlertList
  case object GetStatistics

}