package ws

import java.time.{Clock, Instant}

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

  val request: WSRequest = ws.url(s"https://monitoringapi.solaredge.com/site/${siteId}/overview?api_key=${apiKey}")

  def querySolarEdge():Unit = {
    if(apiKey == null | siteId == null) {
      return
    }
    request.get().map(response => {
      Logger.debug(s"SolarEdge response with status ${response.status}")
      val actualPower = (response.json \ "overview" \ "currentPower" \ "power").as[Double]
      val lastMeasurement = (response.json \ "overview" \ "lastUpdateTime").as[Instant](CommonJsonReadWrite.instantInIso)

      Logger.debug(s"Received response from SolarEdge actualPower: ${actualPower} W; lastMeasurement: ${lastMeasurement}")
      jsonSender.sendRaw("node/garage/pve-inverter/-/power", s"${actualPower},${lastMeasurement.getEpochSecond}")
    }).onFailure {
      case t => Logger.warn(s"An error has occured during querying SolarEdge", t)
    }
  }
}
