package com.thecookiezen.co2.web

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.thecookiezen.co2.domain.Sensor.OK
import com.thecookiezen.co2.domain.{AlertLog, Statistics}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class SensorAPITest extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  val sensorAPI = new SensorAPI(
    storeMeasurement = (_, _) => (),
    getStatus = _ => Future.successful(OK),
    getStatistics = _ => Future.successful(Statistics(20.0, 400)),
    getAlerts = _ => Future.successful(List(AlertLog(0, None, List(1, 2, 3))))
  )

  "SensorAPI" should "return BadRequest for empty json measurement request" in {
    Post("api/v1/sensors/25e05030-a380-466f-94d8-addf6688c462/measurements") ~> sensorAPI.routes ~> check {
      response.status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "return OK for a valid json measurement request" in {
    val jsonRequest = """
         |{
         |  "co2" : 100,
         |  "time":"2020-10-10T18:55:47+00:00"
         |}
        """.stripMargin
    val body = HttpEntity(ContentTypes.`application/json`, jsonRequest)
    Post("api/v1/sensors/25e05030-a380-466f-94d8-addf6688c462/measurements", body) ~> sensorAPI.routes ~> check {
      response.status shouldEqual StatusCodes.OK
    }
  }

  it should "return OK state for the sensor" in {
    Get("api/v1/sensors/25e05030-a380-466f-94d8-addf6688c462") ~> sensorAPI.routes ~> check {
      responseAs[String] shouldEqual """{"status":"OK"}"""
    }
  }

  it should "return statistics for the sensor" in {
    Get("api/v1/sensors/25e05030-a380-466f-94d8-addf6688c462/metrics") ~> sensorAPI.routes ~> check {
      responseAs[String] shouldEqual """{"maxLast30Days":400,"avgLast30Days":20}"""
    }
  }

  it should "return alerts for the sensor" in {
    Get("api/v1/sensors/25e05030-a380-466f-94d8-addf6688c462/alerts") ~> sensorAPI.routes ~> check {
      responseAs[String] shouldEqual """[{"startTime":"1970-01-01T00:00:00Z[UTC]","endTime":null,"measurement1":1,"measurement2":2,"measurement3":3}]"""
    }
  }
}
