package model.actuator.impl

import dao.BySecond
import model.actuator.{Actuator, Command}
import model.location.{Location, LocationRepository}
import model.sensor.{BooleanMeasurementAggregationStrategy, SensorRepository}
import mqtt.JsonSender

/**
  * Represents Bc Relay module
  */
case class BcRelayActuator(
                       locationRepository: LocationRepository,
                       sensorRepository: SensorRepository,
                       jsonSender: JsonSender
                     ) extends Actuator {
  private var fallbackState = false

  override val name: String = "Relay"
  override val location: Location = locationRepository.findOrCreateLocation("836d19833c33")

  override def supportedCommands: Set[Command] = Set(Command("Toggle", Seq.empty))
  override def execute(command: Command): Unit = {
    val measurements = sensorRepository.findOrCreateSensor(location, "relay")
      .findOrCreatePhenomenon("state", "state", BooleanMeasurementAggregationStrategy)
      .measurements(BySecond)

    val newState = if(measurements.nonEmpty) measurements.last.average == 0 else fallbackState
    fallbackState = !newState

    jsonSender.send(
      "node/836d19833c33/relay/0:0/state/set",
      newState
    )
  }
}
