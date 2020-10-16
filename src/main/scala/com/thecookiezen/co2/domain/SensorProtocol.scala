package com.thecookiezen.co2.domain

import java.time.ZonedDateTime

import akka.actor.typed.ActorRef
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.domain.Sensor.SensorState

import scala.concurrent.duration.Duration

object SensorProtocol {
  sealed trait Command
  case class CleanOldSamples(duration: Duration) extends Command
  case class Co2SampleReading(time: ZonedDateTime, measurement: Measurement) extends Command
  case class GetStatus(replyTo: ActorRef[Response]) extends Command
  case class GetAlertList(replyTo: ActorRef[Response]) extends Command
  case class GetStatistics(replyTo: ActorRef[Response]) extends Command
  
  sealed trait Response
  case class StatusResponse(status: SensorState) extends Response
  case class AlertsResponse(alerts: List[AlertLog]) extends Response
  case class StatisticsResponse(statistics: Statistics) extends Response
}
