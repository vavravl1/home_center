package loader

import java.time.LocalDateTime

import akka.actor.Props
import model.actuator.Actuator
import model.sensor.{MeasuredPhenomenon, Sensor}
import model.thermostat.{ActuatorActivator, _}
import play.api.BuiltInComponents

/**
  *
  */
trait ThermostatConfig extends BuiltInComponents with DaoConfig with IfThenConfig {

  def prepareThermostatActuatorActivator(
                                          activatorSensor: Sensor,
                                          activatorPhenomenon: MeasuredPhenomenon,
                                          actuator: Actuator
                                        ) = new ActuatorActivator(
    activatorSensor = activatorSensor,
    activatorPhenomenon = activatorPhenomenon,
    actuator = actuator
  )


  def prepareThermostat(actuatorActivator: ActuatorActivator) = {
    actorSystem.actorOf(Props(
      new Thermostat(
        temperatureSensors = Seq(
          thermometerBedroom
//          thermometerLivingRoom
        ),
        evaluators = Seq(
          SingleSensorThermostatEvaluator(
            timeEvaluator = ThermostatTimeEvaluator(
              startTime = LocalDateTime.of(2017, 1, 1, 5, 30, 0),
              endTime = LocalDateTime.of(2017, 1, 1, 6, 30, 0)
            ),
            valueEvaluator = ThermostatValueEvaluator(20.0, 21.0)
          ),
          SingleSensorThermostatEvaluator(
            timeEvaluator = ThermostatTimeEvaluator(
              startTime = LocalDateTime.of(2017, 1, 2, 17, 30, 0),
              endTime = LocalDateTime.of(2017, 1, 2, 22, 0, 0)
            ),
            valueEvaluator = ThermostatValueEvaluator(20.0, 21.0)
          )
        ),
        defaultCondition = ThermostatValueEvaluator(18.0, 18.5),
        actuatorActivator = actuatorActivator
      )
    ))
  }
}
