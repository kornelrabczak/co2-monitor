package com.thecookiezen.co2.web

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.domain.Sensor._
import com.thecookiezen.co2.web.SensorAPI._
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto.{deriveConfiguredCodec, deriveEnumerationCodec}
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.akkahttp._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.concurrent.{ExecutionContext, Future}

class SensorAPI(storeMeasurement: StoreMeasurement,
                getStatus: GetStatus,
                getStatistics: GetStatistics,
                getAlerts: GetAlertLogs)(implicit ec: ExecutionContext) {

  private val addMeasurementEndpoint: Endpoint[(UUID, SampleJson), Unit, Unit, Any] =
    endpoint
      .post
      .in("api" / "v1" / "sensors" / path[UUID] / "measurements")
      .in(jsonBody[SampleJson])
      .out(emptyOutput)

  private val getStatusEndpoint: Endpoint[UUID, Unit, StatusJson, Any] =
    endpoint
      .get
      .in("api" / "v1" / "sensors" / path[UUID])
      .out(jsonBody[StatusJson])

  private val getStatisticsEndpoint: Endpoint[UUID, Unit, StatisticsJson, Any] =
    endpoint
      .get
      .in("api" / "v1" / "sensors" / path[UUID] / "metrics")
      .out(jsonBody[StatisticsJson])

  private val getAlertsEndpoint: Endpoint[UUID, Unit, List[AlertLogJson], Any] =
    endpoint
      .get
      .in("api" / "v1" / "sensors" / path[UUID] / "alerts")
      .out(jsonBody[List[AlertLogJson]])

  private val openApi = List(addMeasurementEndpoint, getStatusEndpoint, getStatisticsEndpoint, getAlertsEndpoint)
    .toOpenAPI("CO2-Sensor", "1.0")
    .toYaml

  def routes: Route = concat(
    addMeasurementEndpoint.toRoute((handleStoreMeasurement(storeMeasurement) _).tupled),
    getStatusEndpoint.toRoute(handleGetStatus(getStatus)),
    getStatisticsEndpoint.toRoute(handleGetStatistics(getStatistics)),
    getAlertsEndpoint.toRoute(handleGetAlerts(getAlerts)),
    new SwaggerAkka(openApi).routes
  )

  private def handleStoreMeasurement(storeMeasurement: StoreMeasurement)(uuid: UUID, json: SampleJson): Future[Either[Unit, Unit]] = {
    storeMeasurement(uuid, Co2SampleReading(json.time, json.co2))
    Future.successful(Right(()))
  }

  private def handleGetStatus(getStatus: GetStatus)(uuid: UUID): Future[Either[Unit, StatusJson]] = {
    getStatus(uuid).map(state => Right(StatusJson(state)))
  }

  private def handleGetStatistics(getStatistics: GetStatistics)(uuid: UUID): Future[Either[Unit, StatisticsJson]] = {
    getStatistics(uuid).map(stats => Right(StatisticsJson(stats.maxLevel, stats.average.toInt)))
  }

  private def handleGetAlerts(getAlerts: GetAlertLogs)(uuid: UUID): Future[Either[Unit, List[AlertLogJson]]] = {
    getAlerts(uuid).map {
      _.map { alert =>
        AlertLogJson(
          startTime = utcTimestamp2DateTime(alert.startTime),
          endTime = alert.endTime.map(utcTimestamp2DateTime),
          measurement1 = alert.measurements.headOption.getOrElse(0),
          measurement2 = alert.measurements.lift(1).getOrElse(0),
          measurement3 = alert.measurements.lift(2).getOrElse(0)
        )
      }
    }.map(Right(_))
  }
}

object SensorAPI {
  private val DefaultZoneId: ZoneId = ZoneId.of("UTC")

  def utcTimestamp2DateTime: Long => ZonedDateTime =
    t => ZonedDateTime.ofInstant(Instant.ofEpochSecond(t), DefaultZoneId)

  implicit val customServerOptions: AkkaHttpServerOptions =
    AkkaHttpServerOptions.default.copy(logRequestHandling = AkkaHttpServerOptions.default
      .logRequestHandling.copy(logAllDecodeFailures = true))

  implicit val config: Configuration = Configuration.default

  implicit val stateJsonCodec: Codec[SensorState] = deriveEnumerationCodec[SensorState]
  implicit val statusJsonCodec: Codec[StatusJson] = deriveConfiguredCodec[StatusJson]

  case class SampleJson(co2: Measurement, time: ZonedDateTime)

  case class StatusJson(status: SensorState)

  case class StatisticsJson(maxLast30Days: Int, avgLast30Days: Int)

  case class AlertLogJson(startTime: ZonedDateTime,
                          endTime: Option[ZonedDateTime],
                          measurement1: Measurement,
                          measurement2: Measurement,
                          measurement3: Measurement)

}
