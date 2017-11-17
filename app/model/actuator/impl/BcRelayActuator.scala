package model.actuator.impl

import model.actuator.{Actuator, Command}
import model.location.Location
import model.sensor.{BooleanMeasurementAggregationStrategy, Measurement, SensorRepository}
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

  def initialize():Unit = {
    val measurements:Seq[Measurement] = sensorRepository.findOrCreateSensor(location, "relay")
      .findOrCreatePhenomenon("state", "state", BooleanMeasurementAggregationStrategy)
      .lastNMeasurementsDescendant(1)

    state = if(measurements.nonEmpty) measurements.last.average == 0 else false
  }

  override def supportedCommands: Set[Command] = Set(
    Command("Toggle", Seq.empty),
    Command("On", Seq.empty),
    Command("Off", Seq.empty)
  )
  override def execute(command: Command): Unit = {
    command.name match {
      case "Toggle" => state = !state
      case "On" => state = true
      case "Off" => state = false
      case _ => return
    }
    Logger.info(s"BcRelayActuator called and newState is $state")
    jsonSender.send(
      s"node/${location.address}/relay/0:0/state/set",
      if(state) "true" else "false"
    )
  }
}
