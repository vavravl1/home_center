package mqtt.clown

import java.time.Clock

import akka.actor.Actor
import model.{LocationRepository, Measurement, NoneMeasurementAggregationStrategy, SensorRepository}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from big clown
  */
class BridgeListener(sensorRepository: SensorRepository, locationRepository: LocationRepository, clock: Clock) extends Actor {
  val bcSensorTopic = """node/(bridge|remote|base)/(\d+)/([\w-]+)/([\w-]+)""".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, json: JsValue) => receivedTopic match {
      case bcSensorTopic(location, position, sensor, i2cAddress) =>
        Json.fromJson(json)(BcMessage.reads) match {
          case JsSuccess(messages: BcMessage, _) =>
            messages.msgs.foreach(msg => {
              val locationAddress = location + "-" + position
              locationRepository.findOrCreateLocation(locationAddress)
              val foundSensor = sensorRepository.findOrCreateSensor(
                locationAddress = locationAddress,
                name = sensor
              )
              val measurement = Measurement(
                measureTimestamp = clock.instant(),
                value = msg.value
              )
              //              foundSensor.addMeasurement(measurement, msg.phenomenon, msg.unit)

              foundSensor.addMeasurement(measurement, foundSensor.findOrCreatePhenomenon(
                msg.phenomenon, msg.unit, NoneMeasurementAggregationStrategy
              ))
            })
          case JsError(_) => Logger.error(s"Parsing $json failed");
        }
      case _ => {}
    }
  }
}
