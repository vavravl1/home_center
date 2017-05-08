package mqtt.clown

import java.time.Clock

import akka.actor.Actor
import model.{LocationRepository, Measurement, SensorRepository}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from big clown
  */
class BridgeListener(sensorRepository: SensorRepository, locationRepository: LocationRepository, clock: Clock) extends Actor {
  val bcSensorTopic = """nodes/(bridge|remote|base)/(\d+)/([\w-]+)/([\w-]+)""".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, json: JsValue) => receivedTopic match {
      case bcSensorTopic(location, position, sensor, i2cAddress) =>
        Json.fromJson(json)(BcMessage.reads) match {
          case JsSuccess(messages: BcMessage, _) =>
            messages.msgs.foreach(msg => {
              val locationAddress = location + "/" + position
              locationRepository.findOrCreateLocation(locationAddress)
              val foundSensor = sensorRepository.findOrCreateSensor(
                locationAddress = locationAddress,
                name = sensor,
                measuredPhenomenon = msg.phenomenon,
                unit = msg.unit
              )
              val measurement = Measurement(
                measureTimestamp = clock.instant(),
                value = msg.value,
                aggregated = false
              )
              foundSensor.addMeasurement(measurement)
            })
          case JsError(_) => Logger.error(s"Parsing $json failed");
        }
      case _ => {}
    }
  }
}
