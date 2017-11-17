package model.thermostat

import model.actuator.Command
import model.sensor._
import mqtt.listener.SensorMeasurementsListener

class Thermostat(
                  temperatureSensors: Seq[Sensor],
                  evaluator: ThermostatEvaluator,
                  actuatorActivator: ActuatorActivator
                ) extends SensorMeasurementsListener {
  override def messageReceived(
                                sensor: Sensor,
                                phenomenon: MeasuredPhenomenon,
                                measurement: Measurement
                              ): Unit = {
    if (!temperatureSensors.contains(sensor)) return
    if (phenomenon.name != "temperature") return

    evaluator.determineAction match {
      case TurnOn => actuatorActivator.setActualCommand(Command("On", Seq()))
      case TurnOff => actuatorActivator.setActualCommand(Command("Off", Seq()))
      case KeepAsIs => actuatorActivator.setActualCommand(Command("", Seq()))
    }
  }
}

