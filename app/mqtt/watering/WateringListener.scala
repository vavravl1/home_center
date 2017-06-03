package mqtt.watering

import java.time.Clock

import akka.actor.Actor
import entities.watering.WateringMessage
import model._
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
        case JsSuccess(value, _) => {
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
            foundSensor.findOrCreatePhenomenon("humidity", "", NoneMeasurementAggregationStrategy)
          )
          foundSensor.addMeasurement(
            Measurement(
              measureTimestamp = clock.instant(),
              value = if(value.telemetry.watering.inProgress) 10.0 else 0.0
            ),
            foundSensor.findOrCreatePhenomenon("watering", "", BooleanMeasurementAggregationStrategy)
          )
        }
        case JsError(_) => Logger.error(s"Parsing $message failed");
      }
      case _ => {}
    }
  }
}
