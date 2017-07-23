package ws

import java.time.Clock

import model.location.LocationRepository
import model.sensor.{IdentityMeasurementAggregationStrategy, Measurement, SensorRepository}
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class WattmeterClient(
                       ws: WSClient,
                       sensorRepository: SensorRepository,
                       locationRepository: LocationRepository,
                       clock: Clock
                     ) {

  val request: WSRequest = ws.url("http://192.168.100.240/meas.xml")

  def queryWattmeter():Unit = {
    request.get().map(response => {
      val l1Power = (response.xml \ "I1" \ "P").text.toDouble
      val l2Power = (response.xml \ "I2" \ "P").text.toDouble
      val l3Power = (response.xml \ "I3" \ "P").text.toDouble
      val boiler = (response.xml \ "O1" \ "P").text.toDouble

      Logger.debug(s"Received response from wattmeter L1:${l1Power} W, L2: ${l2Power} W, L3: ${l3Power} W")

      val now = clock.instant()

      val sensor = sensorRepository.findOrCreateSensor(
        location = locationRepository.findOrCreateLocation("main-switchboard"),
        name = "wattrouter"
      )

      sensor.addMeasurement(
        measurement = Measurement(l1Power, now),
        measuredPhenomenon = sensor.findOrCreatePhenomenon("L1 Power", "kW", IdentityMeasurementAggregationStrategy)
      )

      sensor.addMeasurement(
        measurement = Measurement(l2Power, now),
        measuredPhenomenon = sensor.findOrCreatePhenomenon("L2 Power", "kW", IdentityMeasurementAggregationStrategy)
      )

      sensor.addMeasurement(
        measurement = Measurement(l3Power, now),
        measuredPhenomenon = sensor.findOrCreatePhenomenon("L3 Power", "kW", IdentityMeasurementAggregationStrategy)
      )

      sensor.addMeasurement(
        measurement = Measurement(boiler, now),
        measuredPhenomenon = sensor.findOrCreatePhenomenon("boiler", "kW", IdentityMeasurementAggregationStrategy)
      )
    })
  }

}
