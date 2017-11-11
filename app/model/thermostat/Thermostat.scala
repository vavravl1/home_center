package model.thermostat

import model.actuator.Command
import model.sensor._
import mqtt.listener.SensorMeasurementsListener

class Thermostat(
                  temperatureSensors: Seq[Sensor],
                  evaluators: Seq[SingleSensorThermostatEvaluator],
                  defaultCondition: ThermostatValueEvaluator,
                  actuatorActivator: ActuatorActivator
                ) extends SensorMeasurementsListener {
  override def messageReceived(
                       sensor: Sensor,
                       phenomenon: MeasuredPhenomenon,
                       measurement: Measurement
                     ): Unit = {
    if(!temperatureSensors.contains(sensor)) return
    if(phenomenon.name != "temperature") return

    val timeMatched = evaluators.filter(_.timeMatch(measurement.measureTimestamp))
    val action = if(timeMatched.isEmpty) {
      defaultCondition.temperatureMatch(measurement.average)
    } else {
      val actions = timeMatched.map(_.temperatureMatch(measurement.average))
      if(actions.contains(TurnOn)) {
        TurnOn
      } else if(actions.forall(_ == TurnOff)) {
        TurnOff
      } else {
        KeepAsIs
      }
    }

    action match {
      case TurnOn => actuatorActivator.setActualCommand(Command("On", Seq()))
      case TurnOff => actuatorActivator.setActualCommand(Command("Off", Seq()))
      case KeepAsIs => actuatorActivator.setActualCommand(Command("", Seq()))
    }
  }
}

