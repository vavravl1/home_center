package ws

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}

import entities.CommonJsonReadWrite
import model.location.LocationRepository
import model.sensor.SensorRepository
import mqtt.JsonSender
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class SolarEdgeClient(
                       ws: WSClient,
                       sensorRepository: SensorRepository,
                       locationRepository: LocationRepository,
                       clock: Clock,
                       jsonSender: JsonSender,
                       apiKey: String,
                       siteId: String
                     ) {
  val timeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  val request: WSRequest = ws.url(s"https://monitoringapi.solaredge.com/site/${siteId}/overview?api_key=${apiKey}")
  val requestStats: WSRequest = ws
    .url(s"https://monitoringapi.solaredge.com/site/${siteId}/energyDetails?api_key=${apiKey}" +
      s"&startTime=${timeFormatter.format(clock.instant().truncatedTo(ChronoUnit.DAYS))}" +
      s"&endTime=${timeFormatter.format(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS))}" +
      s"&timeUnit=DAY&meters=Production")

  def querySolarEdge():Unit = {
    if(apiKey == null | siteId == null) {
      return
    }
    request.get().map(response => {
      if(response.status == 200) {
        val actualPower = (response.json \ "overview" \ "currentPower" \ "power").as[Double]
        val lastMeasurement = clock.instant

        Logger.debug(s"Received response from SolarEdge actualPower: ${actualPower} W; lastMeasurement: ${lastMeasurement}")
        jsonSender.send("node/garage/pve-inverter/-/power", s"${actualPower},${lastMeasurement.getEpochSecond}")
      } else {
        Logger.error(s"Invalid response from SolarEdge: ${response.body}")
      }
    }).onFailure {
      case t => Logger.warn(s"An error has occurred during querying SolarEdge. ", t)
    }

//    Logger.info("url => " + requestStats.url)
//    requestStats.get().map(response => {
//      Logger.info("B => " + response.body)
//      Logger.debug(s"SolarEdge stats response with status ${response.status}")
//      val produced = (response.json \ "energyDetails" \ "meters" \ "values" \ "value").as[Double]
//
//      Logger.info(s"Received response from SolarEdge produced: ${produced} Wh")
//      jsonSender.send("node/garage/pve-inverter/-/power-stats", s"${produced},${clock.instant().getEpochSecond}")
//    }).onFailure {
//      case t => Logger.warn(s"An error has occured during querying SolarEdge", t)
//    }
  }
}
