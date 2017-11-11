package model.thermostat

import model.actuator.{Actuator, Command}
import model.ifthen.{IfThen, TrueCondition}
import model.sensor.{MeasuredPhenomenon, Sensor}
import play.api.Logger

class ActuatorActivator(
                         activatorSensor: Sensor,
                         activatorPhenomenon: MeasuredPhenomenon,
                         actuator: Actuator
                       ) {
  private var actualCommand: Command = new Command("", Seq.empty)

  def setActualCommand(newCommand: Command):Unit = {
    actualCommand = newCommand
    Logger.info(s"Thermostat is set to ${newCommand}")
  }

  def getActualCommand():Command = actualCommand.copy()

  val ifThen = new IfThen(
    objekt = activatorSensor,
    subject = activatorPhenomenon,
    condition = TrueCondition,
    actuator = actuator,
    command = getActualCommand
  )
}
