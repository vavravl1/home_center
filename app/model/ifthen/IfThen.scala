package model.ifthen

import model.actuator.{Actuator, Command}
import model.sensor.{MeasuredPhenomenon, Measurement, Sensor}
import play.api.Logger

/**
  * Represents simple if-then logic. E.g. when a button is pressed, relay is toggled
  */
class IfThen(
              objekt: Sensor,
              subject: MeasuredPhenomenon,
              condition: Condition,
              actuator: Actuator,
              command: Command
            ) {
  def action(sensor: Sensor, phenomenon: MeasuredPhenomenon, measurement: Measurement): Unit = {
    if (sensor.equals(objekt) && subject.equals(phenomenon)) {
      if (condition(phenomenon, measurement)) {
        Logger.info(s"IfThen executed: ${sensor} received ${phenomenon} that was ${measurement}")
        if (command.requiredArguments.nonEmpty) {
          val updatedCommand = updateFirstCommandArgument(phenomenon, measurement)
          actuator.execute(updatedCommand)
        } else {
          actuator.execute(command)
        }
      }
    }
  }

  private def updateFirstCommandArgument(phenomenon: MeasuredPhenomenon, measurement: Measurement) = {
    val value = measurement.average.toString
    val arguments = Seq(command.requiredArguments.head.copy(value = value)) ++ command.requiredArguments.tail
    command.copy(requiredArguments = arguments)
  }
}
