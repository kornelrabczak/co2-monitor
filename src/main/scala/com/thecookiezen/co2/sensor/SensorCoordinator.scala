package com.thecookiezen.co2.sensor

import java.time.{LocalDate, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.sensor.Co2Sensor.{CleanOldSamples, Co2SampleReading, Command}
import com.thecookiezen.co2.sensor.SensorCoordinator.{SensorRequest, isDateFromLast30Days}

import scala.concurrent.duration.{Duration, DurationInt}

class SensorCoordinator(alertThreshold: Measurement) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    import context.dispatcher
    context.system.scheduler.scheduleWithFixedDelay(Duration.Zero, 1.hour, self, CleanOldSamples)
    ()
  }

  override def receive: Receive = {
    case SensorRequest(id: UUID, command: Co2SampleReading) =>
      if (isDateFromLast30Days(command.time))
        forwardMessage(id, command)
    case SensorRequest(id: UUID, command: Command) =>
      forwardMessage(id, command)
    case CleanOldSamples => context.children.foreach { child =>
      child ! CleanOldSamples
    }
  }

  private def forwardMessage(id: UUID, command: Command): Unit = {
    context.child(id.toString) match {
      case Some(child) =>
        log.info("SensorCoordinator forwards message {} to {}", command, id)
        child.tell(command, sender())
      case None =>
        log.info("SensorCoordinator creates new actor and forwards message {} to {}", command, id)
        val child = context.actorOf(Co2Sensor.props(id, alertThreshold), id.toString)
        child.tell(command, sender())
    }
  }
}

object SensorCoordinator {
  private val DefaultZoneId: ZoneId = ZoneId.of("UTC")

  def props(alertThreshold: Measurement): Props = Props(new SensorCoordinator(alertThreshold))

  def isDateFromLast30Days(time: ZonedDateTime): Boolean = {
    val thirtyDaysAgo = LocalDate.now(DefaultZoneId).minusDays(31)
    time.toLocalDate.isAfter(thirtyDaysAgo)
  }

  case class SensorRequest(id: UUID, command: Command)
}
