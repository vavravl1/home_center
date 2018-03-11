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
      aggregationStrategy = getAggregationStrategy(value, unit)
    )
    val measurement = Measurement(
      measureTimestamp = measureTimestamp,
      value = getTypedValue(measuredPhenomenon, value)
    )

    (sensor, phenomenon, measurement)
  }

  private def getTypedValue(measuredPhenomenon: String, value: String) = {
    if (isDouble(value)) {
      value.toDouble * MeasuredPhenomenonScale(measuredPhenomenon)
    } else if (isBoolean(value)) {
      value.toBoolean
    } else {
      value
    }
  }

  private def getAggregationStrategy(value: String, unit: String) = {
    if (isDouble(value)) {
      unit match {
        case "kWh" => SingleValueAggregationStrategy
        case _ => DoubleValuesMeasurementAggregationStrategy
      }
    } else {
      EnumeratedMeasurementAggregationStrategy
    }
  }

  private def parseMessage(message: String): (String, Instant) = {
    val splicedMessage = message.split(",")
    val value = splicedMessage(0)
    val measureTimestamp = if (splicedMessage.length == 2)
      Instant.ofEpochSecond(splicedMessage(1).toLong)
    else clock.instant()
    (value, measureTimestamp)
  }

  private def isDouble(value: String): Boolean = {
    try {
      value.toDouble
      true
    } catch {
      case _: Throwable => false
    }
  }

  private def isBoolean(value: String): Boolean = {
    value == "true" || value == "false"
  }
}
