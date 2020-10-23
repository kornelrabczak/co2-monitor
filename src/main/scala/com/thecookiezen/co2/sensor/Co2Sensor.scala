package com.thecookiezen.co2.sensor

import java.time._

import akka.actor.{Actor, ActorLogging, Props}
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.domain.Sensor._
import com.thecookiezen.co2.domain.{AlertLog, Co2Sample, Statistics}
import com.thecookiezen.co2.sensor.Co2Sensor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

class Co2Sensor(alertThreshold: Measurement) extends Actor with ActorLogging {

  private val alertLogs: ListBuffer[AlertLog] = ListBuffer.empty
  private var measurements: ListBuffer[Co2Sample] = ListBuffer.empty

  log.info("Device actor {} started", self.path.name)

  override def receive: Receive = oKwithData.orElse(handleQueries)

  private def oKwithData: Receive = {
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      measurements += sample
      if (sample.sample > alertThreshold) {
        val alert = AlertLog(startTime = sample.utcTimestamp, measurements = List(sample.sample))
        changeState(warning(okCounter = 0, alertCounter = 1, possibleAlertLog = alert))
      }
    case GetStatus =>
      log.info("Status OK on {} device", self.path.name)
      sender() ! OK
  }

  private def warning(okCounter: Int, alertCounter: Int, possibleAlertLog: AlertLog): Receive = {
    case GetStatus =>
      log.info("Status WARNING on {} device", self.path.name)
      sender() ! WARN
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
    case GetStatus =>
      log.info("Status ALERT on {} device", self.path.name)
      sender() ! ALERT
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
        val fromDate = ZonedDateTime.now(DefaultZoneId).minusDays(keepDays + 1)
        val sampleDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(sample.utcTimestamp), DefaultZoneId)
        sampleDate.isAfter(fromDate)
      }
  }

  private def changeState(to: Receive): Unit = {
    context.become(to.orElse(handleQueries))
  }
}

object Co2Sensor {
  private val DefaultZoneId: ZoneId = ZoneId.of("UTC");

  def props(alertThreshold: Measurement): Props = Props(new Co2Sensor(alertThreshold))

  case class CleanOldSamples(duration: Duration)
}