package com.thecookiezen.co2.sensor

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.thecookiezen.co2.domain.Statistics
import com.thecookiezen.co2.sensor.Co2Sensor.{Co2SampleReading, GetStatistics}
import com.thecookiezen.co2.sensor.SensorCoordinator.SensorRequest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SensorCoordinatorTest extends TestKit(ActorSystem())
  with AnyFlatSpecLike
  with Matchers
  with BeforeAndAfterAll {

  implicit val sender = testActor

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "SensorCoordinator" should "create new sensor if it doesn't exist and forward messages" in {
    val coordinator = system.actorOf(SensorCoordinator.props(1000))

    val sensor1 = UUID.randomUUID()
    val sensor2 = UUID.randomUUID()
    val sensor3 = UUID.randomUUID()

    coordinator ! SensorRequest(sensor1, Co2SampleReading(LocalDateTime.now(), 10))
    coordinator ! SensorRequest(sensor1, Co2SampleReading(LocalDateTime.now(), 20))
    coordinator ! SensorRequest(sensor3, Co2SampleReading(LocalDateTime.now(), 30))

    coordinator ! SensorRequest(sensor1, GetStatistics)
    expectMsg[Statistics](Statistics(15.0, 20))

    coordinator ! SensorRequest(sensor2, GetStatistics)
    expectMsg[Statistics](Statistics.empty)

    coordinator ! SensorRequest(sensor3, GetStatistics)
    expectMsg[Statistics](Statistics(30.0, 30))
  }
}
