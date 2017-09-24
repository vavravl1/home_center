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
  def action(sensor: Sensor, phenomenon: MeasuredPhenomenon, measurement: Measurement):Unit = {
    if(sensor.equals(objekt) && subject.equals(phenomenon)) {
      if(condition(phenomenon, measurement)) {
        Logger.info(s"IfThen executed: ${sensor} received ${phenomenon} that was ${measurement}")
        actuator.execute(command)
      }
    }
  }
}
