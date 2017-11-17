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
          thermometerBedroom,
          thermometerLivingRoom
        ),
        ThermostatEvaluator(
          Seq(
            createSingleSensorThermostatEvaluator(
              thermometerBedroom,
              LocalDateTime.of(2017, 1, 1, 0, 0, 0),
              LocalDateTime.of(2017, 1, 1, 5, 30, 0),
              17
            ),
            createSingleSensorThermostatEvaluator(
              thermometerBedroom,
              LocalDateTime.of(2017, 1, 1, 5, 30, 0),
              LocalDateTime.of(2017, 1, 1, 6, 30, 0),
              20.0
            ),
            createSingleSensorThermostatEvaluator(
              thermometerBedroom,
              LocalDateTime.of(2017, 1, 1, 6, 30, 0),
              LocalDateTime.of(2017, 1, 1, 21, 40, 0),
              17
            ),
            createSingleSensorThermostatEvaluator(
              thermometerBedroom,
              LocalDateTime.of(2017, 1, 1, 21, 40, 0),
              LocalDateTime.of(2017, 1, 1, 22, 20, 0),
              20
            ),
            createSingleSensorThermostatEvaluator(
              thermometerLivingRoom,
              LocalDateTime.of(2017, 1, 1, 17, 30, 0),
              LocalDateTime.of(2017, 1, 1, 22, 0, 0),
              20.0
            )
          ),
          defaultCondition = ThermostatValueEvaluator(18.0, 18.5)
        ),
        actuatorActivator = actuatorActivator
      )
    ))
  }

  private def createSingleSensorThermostatEvaluator(
                                             sensor: Sensor,
                                             startTime: LocalDateTime,
                                             endTime: LocalDateTime,
                                             minimalTemperature: Double
                                           ): SingleSensorThermostatEvaluator = {
    SingleSensorThermostatEvaluator(
      temperatureSensor = sensor,
      timeEvaluator = ThermostatTimeEvaluator(
        startTime = startTime,
        endTime = endTime
      ),
      valueEvaluator = ThermostatValueEvaluator(minimalTemperature, minimalTemperature*1.05)
    )
  }
}
