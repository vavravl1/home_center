package mqtt.clown

import akka.actor.Actor
import model.sensor._
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}

/**
  * Stores messages from big clown
  */
class BigClownStoringListener(
                      mqttBigClownParser: MqttBigClownParser
                    ) extends Actor {
  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, message: String) =>
      mqttBigClownParser.parseMqttMessage(receivedTopic, message)
        .map({case (sensor, phenomenon, measurement) => storeSensorMeasurement(sensor, phenomenon, measurement)})
    case _ =>
  }

  private def storeSensorMeasurement(sensor: Sensor, phenomenon: MeasuredPhenomenon, measurement: Measurement) = {
    sensor.addMeasurement(measurement, phenomenon)
  }
}
