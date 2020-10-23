//package com.thecookiezen.co2.sensor
//
//import java.time.ZonedDateTime
//
//import akka.actor.ActorSystem
//import akka.testkit.TestKit
//import com.thecookiezen.co2.domain.Sensor._
//import com.thecookiezen.co2.domain.{AlertLog, Statistics}
//import com.thecookiezen.co2.sensor.Co2Sensor._
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.flatspec.AnyFlatSpecLike
//import org.scalatest.matchers.should.Matchers
//
//import scala.concurrent.duration.DurationInt
//
//class Co2SensorTest
//  extends TestKit(ActorSystem())
//    with AnyFlatSpecLike
//    with Matchers
//    with BeforeAndAfterAll {
//
//  implicit val sender = testActor
//
//  override def afterAll(): Unit = {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  "Co2Sensor" should "return empty values after start" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    sensor ! GetAlertList
//    expectMsg[List[AlertLog]](List.empty)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](OK)
//
//    sensor ! GetStatistics
//    expectMsg[Statistics](Statistics.empty)
//  }
//
//  it should "return average and maximum statistics for list of samples" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 10)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 30)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 20)
//
//    sensor ! GetStatistics
//    expectMsg[Statistics](Statistics(20.0, 30))
//  }
//
//  it should "move from ok to warning state after sample above threshold" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 10)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](WARN)
//  }
//
//  it should "move from warning to ok state after getting 3 consecutive samples below threshold" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](WARN)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 200)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](WARN)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 500)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 300)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](OK)
//  }
//
//  it should "move from warning to alert state after getting 3 consecutive samples above threshold" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](WARN)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 200)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](WARN)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](ALERT)
//  }
//
//  it should "move from alert to ok state after getting 3 consecutive samples below threshold and create alert log" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    val firstEventTime = ZonedDateTime.now()
//    val firstSampleAboveThreshold = Co2SampleReading(ZonedDateTime.now(), 2500)
//    val secondSampleAboveThreshold = Co2SampleReading(ZonedDateTime.now(), 2500)
//    val thirdSampleAboveThreshold = Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! firstSampleAboveThreshold
//    sensor ! secondSampleAboveThreshold
//    sensor ! thirdSampleAboveThreshold
//
//    sensor ! GetStatus
//    expectMsg[SensorState](ALERT)
//
//    sensor ! GetAlertList
//    expectMsg[List[AlertLog]](List(AlertLog(
//      startTime = firstEventTime.toEpochSecond,
//      endTime = None,
//      measurements = List(firstSampleAboveThreshold.measurement,
//        secondSampleAboveThreshold.measurement,
//        thirdSampleAboveThreshold.measurement)
//    )))
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 10)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 2500)
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 15)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 20)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](ALERT)
//
//    val lastEventTimeBeforeAlertIsFinished = ZonedDateTime.now()
//
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 10)
//    sensor ! Co2SampleReading(ZonedDateTime.now(), 15)
//    sensor ! Co2SampleReading(lastEventTimeBeforeAlertIsFinished, 20)
//
//    sensor ! GetStatus
//    expectMsg[SensorState](OK)
//
//    sensor ! GetAlertList
//    expectMsg[List[AlertLog]](List(AlertLog(
//      startTime = firstEventTime.toEpochSecond,
//      endTime = Some(lastEventTimeBeforeAlertIsFinished.toEpochSecond),
//      measurements = List(firstSampleAboveThreshold.measurement,
//        secondSampleAboveThreshold.measurement,
//        thirdSampleAboveThreshold.measurement)
//    )))
//  }
//
//  it should "clean old samples" in {
//    val sensor = system.actorOf(Co2Sensor.props(1000))
//
//    val today = ZonedDateTime.now()
//    val weekAgo = today.minusDays(7)
//
//    sensor ! Co2SampleReading(today, 10)
//    sensor ! Co2SampleReading(today, 20)
//    sensor ! Co2SampleReading(today, 30)
//    sensor ! Co2SampleReading(weekAgo, 500)
//
//    sensor ! CleanOldSamples(3.days)
//
//    sensor ! GetStatistics
//    expectMsg[Statistics](Statistics(20.0, 30))
//  }
//}
