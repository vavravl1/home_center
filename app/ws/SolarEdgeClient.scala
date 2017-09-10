package ws

import java.time.{Clock, Instant}

import entities.CommonJsonReadWrite
import model.location.LocationRepository
import model.sensor.{IdentityMeasurementAggregationStrategy, Measurement, SensorRepository}
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
                       apiKey: String,
                       siteId: String
                     ) {

  val request: WSRequest = ws.url(s"https://monitoringapi.solaredge.com/site/${siteId}/overview?api_key=${apiKey}")

  def querySolarEdge():Unit = {
    request.get().map(response => {
      val actualPower = (response.json \ "overview" \ "currentPower" \ "power").as[Double]
      val lastMeasurement = (response.json \ "overview" \ "lastUpdateTime").as[Instant](CommonJsonReadWrite.instantInIso)

      Logger.debug(s"Received response from SolarEdge actualPower: ${actualPower} W; lastMeasurement: ${lastMeasurement}")

      val sensor = sensorRepository.findOrCreateSensor(
        location = locationRepository.findOrCreateLocation("garage"),
        name = "pve-inverter"
      )

      sensor.addMeasurement(
        measurement = Measurement(actualPower, lastMeasurement),
        measuredPhenomenon = sensor.findOrCreatePhenomenon("Power", "w", IdentityMeasurementAggregationStrategy)
      )
    })
  }
}
