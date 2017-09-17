package model.ifthen

import java.time.{Clock, Instant}

import akka.actor.Actor
import model.location.LocationRepository
import model.sensor.{BooleanMeasurementAggregationStrategy, IdentityMeasurementAggregationStrategy, Measurement, SensorRepository}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import mqtt.clown.{MeasuredPhenomenonScale, MeasuredPhenomenonToUnit}

/**
  * Class responsible for execution of if-thens based on mqtt messages
  */
class MqttIfThenExecutor(
                    sensorRepository: SensorRepository,
                    locationRepository: LocationRepository,
                    clock: Clock,
                    ifThens: Seq[IfThen]
                  ) extends Actor {
  //node/836d19833c33/thermometer/0:0/temperature received 24.56
  //node/836d19833c33/relay/0:0/state/set
  val bcSensorTopic = """node/([\w-]+)/([\w-]+)/(\d):(\d)/(\w+)/?\w?""".r
  val bcSensorTopicWithoutI2d = """node/([\w-]+)/([\w-]+)/-/([\w-]+)""".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, message: String) => receivedTopic match {
      case bcSensorTopic(nodeId, sensor, addr1, addr2, measuredPhenomenon) =>
        evaluateMessage(message, nodeId, sensor, measuredPhenomenon)
      case bcSensorTopicWithoutI2d(nodeId, sensor, measuredPhenomenon) =>
        evaluateMessage(message, nodeId, sensor, measuredPhenomenon)
      case _ => {}
    }
    case _ => {}
  }

  private def evaluateMessage(message: String, nodeId: String, sensorName: String, measuredPhenomenon: String) = {
    val (value: String, measureTimestamp: Instant) = parseMessage(message)
    val location = locationRepository.findOrCreateLocation(nodeId)
    val sensor = sensorRepository.findOrCreateSensor(
      location = location,
      name = sensorName
    )
    val phenomenon = sensor.findOrCreatePhenomenon(
      name = measuredPhenomenon,
      unit = MeasuredPhenomenonToUnit(measuredPhenomenon),
      aggregationStrategy = value match {
        case "true" => BooleanMeasurementAggregationStrategy
        case "false" => BooleanMeasurementAggregationStrategy
        case _ => IdentityMeasurementAggregationStrategy
      }
    )
    val measurement = Measurement(
      measureTimestamp = measureTimestamp,
      value = MeasuredPhenomenonScale(measuredPhenomenon) * (value match {
        case "true" => 10
        case "false" => 0
        case _ => value.toDouble
      })
    )

    ifThens.foreach(ifThen => ifThen.action(sensor, phenomenon, measurement))
  }

  private def parseMessage(message: String):(String, Instant) = {
    val splicedMessage = message.split(",")
    val value = splicedMessage(0)
    val measureTimestamp = if (splicedMessage.length == 2)
      Instant.ofEpochSecond(splicedMessage(1).toLong)
    else clock.instant()
    (value, measureTimestamp)
  }
}
