package mqtt.watering

import java.time.Clock

import akka.actor.Actor
import model._
import model.sensor.{BooleanMeasurementAggregationStrategy, IdentityMeasurementAggregationStrategy, Measurement, SensorRepository}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}

/**
  * Stores messages from watering to dao
  */
class WateringListener(
                        locationRepository: LocationRepository,
                        sensorRepository: SensorRepository,
                        clock: Clock
                      ) extends Actor {
  val topic = "home/watering/ibisek/telemetry".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, message: String) => receivedTopic match {
      case topic() => Json.fromJson(Json.parse(message))(WateringMessage.wmReads) match {
        case JsSuccess(value, _) =>
          val locationAddress = "watering-ibiscus"
          val location = locationRepository.findOrCreateLocation(locationAddress)
          val foundSensor = sensorRepository.findOrCreateSensor(
            location = location,
            name = "watering"
          )
          foundSensor.addMeasurement(
            Measurement(
              measureTimestamp = clock.instant(),
              value = value.telemetry.humidity.actual
            ),
            foundSensor.findOrCreatePhenomenon("humidity", "", IdentityMeasurementAggregationStrategy)
          )
          foundSensor.addMeasurement(
            Measurement(
              measureTimestamp = clock.instant(),
              value = if(value.telemetry.watering.inProgress) 10.0 else 0.0
            ),
            foundSensor.findOrCreatePhenomenon("watering", "", BooleanMeasurementAggregationStrategy)
          )
          foundSensor.addMeasurement(
            Measurement(
              measureTimestamp = clock.instant(),
              value = value.telemetry.humidity.baseLine
            ),
            foundSensor.findOrCreatePhenomenon("baseLine", "", IdentityMeasurementAggregationStrategy)
          )
          foundSensor.addMeasurement(
            Measurement(
              measureTimestamp = clock.instant(),
              value = if(value.telemetry.waterLevelHigh) 10.0 else 0.0
            ),
            foundSensor.findOrCreatePhenomenon("waterLevel", "", IdentityMeasurementAggregationStrategy)
          )
        case JsError(_) => Logger.error(s"Parsing $message failed");
      }
      case _ =>
    }
  }
}
