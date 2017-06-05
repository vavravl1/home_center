package mqtt.clown

import java.time.Clock

import akka.actor.Actor
import model.{LocationRepository, Measurement, NoneMeasurementAggregationStrategy, SensorRepository}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}

/**
  * Stores messages from big clown
  */
class BridgeListener(sensorRepository: SensorRepository, locationRepository: LocationRepository, clock: Clock) extends Actor {
  //node/836d19833c33/thermometer/0:0/temperature received 24.56
  val bcSensorTopic = """node/([\w-]+)/([\w-]+)/(\d):(\d)/(\w+)""".r
  val bcSensorTopicWithoutI2d = """node/([\w-]+)/([\w-]+)/-/([\w-]+)""".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, message: String) => receivedTopic match {
      case bcSensorTopic(nodeId, sensor, addr1, addr2, measuredPhenomenon) =>
        storeSensorMeasurement(message, nodeId, sensor, measuredPhenomenon)
      case bcSensorTopicWithoutI2d(nodeId, sensor, measuredPhenomenon) =>
        storeSensorMeasurement(message, nodeId, sensor, measuredPhenomenon)
      case _ => {}
    }
    case _ => {}
  }

  private def storeSensorMeasurement(message: String, nodeId: String, sensor: String, measuredPhenomenon: String) = {
    val location = locationRepository.findOrCreateLocation(nodeId)
    val foundSensor = sensorRepository.findOrCreateSensor(
      location = location,
      name = sensor
    )
    foundSensor.addMeasurement(
      Measurement(
        measureTimestamp = clock.instant(),
        value = MeasuredPhenomenonScale(measuredPhenomenon) * message.toDouble
      ),
      foundSensor.findOrCreatePhenomenon(
        measuredPhenomenon, MeasuredPhenomenonToUnit(measuredPhenomenon), NoneMeasurementAggregationStrategy
      ))
  }
}
