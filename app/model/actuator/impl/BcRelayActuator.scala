package model.actuator.impl

import dao.BySecond
import model.actuator.{Actuator, Command}
import model.location.Location
import model.sensor.{BooleanMeasurementAggregationStrategy, SensorRepository}
import mqtt.JsonSender
import play.api.Logger

/**
  * Represents Bc Relay module
  */
case class BcRelayActuator(
                       location: Location,
                       sensorRepository: SensorRepository,
                       jsonSender: JsonSender
                     ) extends Actuator {
  private var state = false

  override val name: String = "Relay"

  def initialize:Unit = {
    val measurements = sensorRepository.findOrCreateSensor(location, "relay")
      .findOrCreatePhenomenon("state", "state", BooleanMeasurementAggregationStrategy)
      .measurements(BySecond)

    state = if(measurements.nonEmpty) measurements.last.average == 0 else false
  }

  override def supportedCommands: Set[Command] = Set(Command("Toggle", Seq.empty))
  override def execute(command: Command): Unit = {
    state = !state
    Logger.info(s"BcRelayActuator called and newState is ${state}")
    jsonSender.send(
      s"node/${location.address}/relay/0:0/state/set",
      if(state) "true" else "false"
    )
  }
}
