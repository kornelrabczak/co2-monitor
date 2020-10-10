package com.thecookiezen.co2.sensor

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.sensor.Co2Sensor.{CleanOldSamples, Command}
import com.thecookiezen.co2.sensor.SensorCoordinator.SensorRequest

import scala.concurrent.duration.{Duration, DurationInt}

class SensorCoordinator(alertThreshold: Measurement) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    import context.dispatcher
    context.system.scheduler.scheduleWithFixedDelay(Duration.Zero, 1.hour, self, CleanOldSamples)
    ()
  }

  override def receive: Receive = {
    case SensorRequest(id: UUID, command: Command) =>
      context.child(id.toString) match {
        case Some(child) =>
          log.info("SensorCoordinator forwards message {} to {}", command, id)
          child.tell(command, sender())
        case None =>
          log.info("SensorCoordinator creates new actor and forwards message {} to {}", command, id)
          val child = context.actorOf(Co2Sensor.props(id, alertThreshold), id.toString)
          child.tell(command, sender())
      }
    case CleanOldSamples => context.children.foreach { child =>
      child ! CleanOldSamples
    }
  }
}

object SensorCoordinator {
  private val DefaulZoneId: ZoneId = ZoneId.of("UTC");

  def props(alertThreshold: Measurement): Props = Props(new SensorCoordinator(alertThreshold))

  def isDateFromLast30Days(time: LocalDateTime): Boolean = {
    val thirtyDaysAgo = LocalDate.now(DefaulZoneId).minusDays(31)
    time.atZone(DefaulZoneId).toLocalDate.isAfter(thirtyDaysAgo)
  }

  case class SensorRequest(id: UUID, command: Command)
}
