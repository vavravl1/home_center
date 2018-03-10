package mqtt.clown

import java.time.{Clock, Instant}

import model.location.LocationRepository
import model.sensor._

/**
  * Parses mqtt messages from BigClown ecosystem
  */
class MqttBigClownParser(
                          sensorRepository: SensorRepository,
                          locationRepository: LocationRepository,
                          clock: Clock
                        ) {
  //node/836d19833c33/thermometer/0:0/temperature received 24.56
  //node/836d19833c33/hygrometer/0:4/relative-humidity
  //node/836d19833c33/battery/%s/voltage
  val bcSensorTopic = """node/([\w-]+)/([\w-]+)/(\d):(\d)/([\w-]+)/?\w?""".r
  val bcSensorTopicWithoutI2d = """node/([\w-]+)/([\w-]+)/-/([\w-]+)""".r
  val bcSensorBattery = """node/([\w-]+)/([\w-]+)/(standard|mini)/([\w-]+)""".r

  def parseMqttMessage(receivedTopic: String, message: String):Option[(Sensor, MeasuredPhenomenon, Measurement)] =
    receivedTopic match {
      case bcSensorTopic(nodeId, sensor, addr1, addr2, measuredPhenomenon) =>
        Some(evaluateMessage(message, nodeId, sensor, measuredPhenomenon))
      case bcSensorTopicWithoutI2d(nodeId, sensor, measuredPhenomenon) =>
        Some(evaluateMessage(message, nodeId, sensor, measuredPhenomenon))
      case bcSensorBattery(nodeId, sensor, batteryType, measuredPhenomenon) =>
        Some(evaluateMessage(message, nodeId, sensor, measuredPhenomenon + "_" + batteryType))
      case _ => None
    }

  private def evaluateMessage(message: String, nodeId: String, sensorName: String, measuredPhenomenon: String)
  :(Sensor, MeasuredPhenomenon, Measurement) = {
    val (value: String, measureTimestamp: Instant) = parseMessage(message)
    val location = locationRepository.findOrCreateLocation(nodeId)
    val sensor = sensorRepository.findOrCreateSensor(
      location = location,
      name = sensorName
    )
    val unit = MeasuredPhenomenonToUnit(measuredPhenomenon)
    val phenomenon = sensor.findOrCreatePhenomenon(
      name = measuredPhenomenon,
      unit = unit,
      aggregationStrategy = value match {
        case "true" => BooleanMeasurementAggregationStrategy
        case "false" => BooleanMeasurementAggregationStrategy
        case _ => unit match {
          case "kWh" => SingleValueAggregationStrategy
          case _ => DoubleValuesMeasurementAggregationStrategy
        }
      }
    )
    val measurement = Measurement(
      measureTimestamp = measureTimestamp,
      value = MeasuredPhenomenonScale(measuredPhenomenon) * (value match {
        case "true" => 10
        case "false" => 0
        case """"up-stop"""" => 20
        case """"up"""" => 15
        case """"down"""" => 10
        case """"down-stop"""" => 0
        case _ => value.toDouble
      })
    )

    return (sensor, phenomenon, measurement)
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
