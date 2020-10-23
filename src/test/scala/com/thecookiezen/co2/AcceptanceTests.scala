//package com.thecookiezen.co2
//
//import java.util.UUID
//
//import akka.http.scaladsl.model.HttpMethods.POST
//import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//class AcceptanceTests
//  extends AnyFlatSpec
//    with Matchers
//    with ScalatestRouteTest {
//
//  "Sensor monitor" should "gather measurments from 3 different sensors and record their states properly" in {
//    val sensor1 = UUID.randomUUID()
//    val sensor2 = UUID.randomUUID()
//    val sensor3 = UUID.randomUUID()
//
//    createMeasurementRequest(sensor1, 10, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    createMeasurementRequest(sensor1, 20, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    createMeasurementRequest(sensor3, 30, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    Get(s"api/v1/sensors/${sensor1.toString}/metrics") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"maxLast30Days":15,"avgLast30Days":20}"""
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}/metrics") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"maxLast30Days":0,"avgLast30Days":0}"""
//    }
//
//    Get(s"api/v1/sensors/${sensor3.toString}/metrics") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"maxLast30Days":30,"avgLast30Days":30}"""
//    }
//
//    createMeasurementRequest(sensor2, 4000, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    createMeasurementRequest(sensor2, 5000, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"status":"WARN"}"""
//    }
//
//    createMeasurementRequest(sensor2, 8000, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"status":"ALERT"}"""
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}/alerts") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """[{"startTime":"2020-10-10T18:55:47Z[UTC]","endTime":null,"measurement1":4000,"measurement2":5000,"measurement3":8000}]"""
//    }
//
//    createMeasurementRequest(sensor2, 55, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    createMeasurementRequest(sensor2, 11, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    createMeasurementRequest(sensor2, 5, "2020-10-10T18:55:47+00:00") ~> Co2App.routes ~> check {
//      response.status shouldEqual StatusCodes.OK
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """{"status":"OK"}"""
//    }
//
//    Get(s"api/v1/sensors/${sensor2.toString}/alerts") ~> Co2App.routes ~> check {
//      responseAs[String] shouldEqual """[{"startTime":"2020-10-10T18:55:47Z[UTC]","endTime":"2020-10-10T18:55:47Z[UTC]","measurement1":4000,"measurement2":5000,"measurement3":8000}]"""
//    }
//  }
//
//  private def createMeasurementRequest(uuid: UUID, co2: Int, time: String) = {
//    val jsonRequest =
//      s"""
//         |{
//         |  "co2" : $co2,
//         |  "time": "$time"
//         |}
//        """.stripMargin
//    val body = HttpEntity(ContentTypes.`application/json`, jsonRequest)
//    new RequestBuilder(POST)(s"api/v1/sensors/${uuid}/measurements", body)
//  }
//}
