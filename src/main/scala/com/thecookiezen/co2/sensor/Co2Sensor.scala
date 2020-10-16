package com.thecookiezen.co2.sensor

import java.time._
import java.util.UUID

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.domain.Sensor._
import com.thecookiezen.co2.domain.SensorProtocol._
import com.thecookiezen.co2.domain.{AlertLog, Co2Sample, Statistics}
import com.thecookiezen.co2.sensor.Co2Sensor._

import scala.collection.mutable.ListBuffer

class Co2Sensor(context: ActorContext[Command], id: UUID, alertThreshold: Measurement) extends AbstractBehavior[Command](context) {

  context.log.info("Device actor {} started", id)

  override def onMessage(msg: Command): Behavior[Command] = okData(Data())

  private def okData(data: Data) = Behaviors.receiveMessage[Command] {
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      data.measurements += sample
      if (sample.sample > alertThreshold) {
        val alert = AlertLog(startTime = sample.utcTimestamp, measurements = List(sample.sample))
        warning(data.copy(status = WARN), WarningCounter(okCounter = 0, alertCounter = 1, possibleAlertLog = alert))
      } else {
        Behaviors.same
      }
  }
  
  private def warning(data: Data, counter: WarningCounter) = Behaviors.receiveMessage[Command] {
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      data.measurements += sample

      if (sample.sample > alertThreshold) {
        val alertLog = counter.possibleAlertLog.copy(measurements = counter.possibleAlertLog.measurements :+ sample.sample)
        if (counter.alertCounter + 1 > 2) {
          alert(data.copy(status = ALERT), AlertCounter(0, alertLog))
        } else {
          warning(data, counter.copy(okCounter = 0, alertCounter = counter.alertCounter + 1, possibleAlertLog = alertLog))
        }
      } else {
        if (counter.okCounter + 1 > 2) {
          okData(data.copy(status = OK))
        } else {
          warning(counter.copy(okCounter = counter.okCounter + 1, 0, counter.possibleAlertLog.copy(measurements = List.empty)))
        }
      }
  }

  private def alert(data: Data, counter: AlertCounter): Behaviors.Receive[Command] = Behaviors.receiveMessage[Command] {
    case GetAlertList(replyTo) => 
      replyTo ! AlertsResponse(data.alertLogs.toList :+ counter.alertLog)
      Behaviors.same
    case reading: Co2SampleReading =>
      val sample = Co2Sample.fromReading(reading)
      data.measurements += sample

      if (sample.sample > alertThreshold) {
        alert(data, counter.copy(okCounter = 0))
      } else {
        if (counter.okCounter + 1 > 2) {
          data.alertLogs += counter.alertLog.copy(endTime = Some(sample.utcTimestamp))
          okData(data.copy(status = OK))
        } else {
          alert(data, counter.copy(okCounter = counter.okCounter + 1))
        }
      }
  }

  private def queryBehaviour(data: Data) = Behaviors.receiveMessage[Command] {
    case GetStatus(replyTo) =>
      replyTo ! StatusResponse(data.status)
      Behaviors.unhandled
    case GetAlertList(replyTo) => 
      replyTo ! AlertsResponse(data.alertLogs.toList)
      Behaviors.unhandled
    case GetStatistics(replyTo) =>
      (Co2Sample.average(data.measurements.toList), Co2Sample.max(data.measurements.toList)) match {
        case (Some(avg), Some(max)) => replyTo ! StatisticsResponse(Statistics(avg, max.sample))
        case _ => replyTo ! StatisticsResponse(Statistics.empty)
      }
      Behaviors.unhandled
    case CleanOldSamples(duration) =>
      val keepDays = duration.toDays
      data.copy(measurements = data.measurements.filter { sample =>
        val fromDate = ZonedDateTime.now(DefaultZoneId).minusDays(keepDays + 1)
        val sampleDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(sample.utcTimestamp), DefaultZoneId)
        sampleDate.isAfter(fromDate)
      })
      Behaviors.unhandled
  }
}

object Co2Sensor {
  private val DefaultZoneId: ZoneId = ZoneId.of("UTC")

  case class Data(status: SensorState = OK,
                  alertLogs: ListBuffer[AlertLog] = ListBuffer.empty, 
                  measurements: ListBuffer[Co2Sample] = ListBuffer.empty)
  case class WarningCounter(okCounter: Int, alertCounter: Int, possibleAlertLog: AlertLog)
  case class AlertCounter(okCounter: Int, alertLog: AlertLog)
  
  def apply(id: UUID, alertThreshold: Measurement): Behavior[Command] = Behaviors.setup(context => {
    context.setLoggerName("Co2Sensor")
    new Co2Sensor(context, id, alertThreshold)
  })
}
