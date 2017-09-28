package mqtt.clown

import model.sensor._
import mqtt.MqttListener

/**
  * Stores messages from big clown
  */
class BigClownStoringListener(
                      mqttBigClownParser: MqttBigClownParser
                    ) extends MqttListener {
  override def messageReceived(receivedTopic: String, message: String): Unit =
    mqttBigClownParser.parseMqttMessage(receivedTopic, message)
      .map({case (sensor, phenomenon, measurement) => storeSensorMeasurement(sensor, phenomenon, measurement)})

  private def storeSensorMeasurement(sensor: Sensor, phenomenon: MeasuredPhenomenon, measurement: Measurement) = {
    sensor.addMeasurement(measurement, phenomenon)
  }
}
