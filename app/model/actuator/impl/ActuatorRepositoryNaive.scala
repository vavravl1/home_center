package model.actuator.impl

import model.actuator.{Actuator, ActuatorRepository}
import model.location.{Location, LocationRepository}
import model.sensor.SensorRepository
import mqtt.JsonSender

/**
  *
  */
class ActuatorRepositoryNaive(
                               locationRepository: LocationRepository,
                               sensorRepository: SensorRepository,
                               jsonSender: JsonSender
                             ) extends ActuatorRepository {

  private val actuators = Seq(
    new BcRelayActuator(
      locationRepository = locationRepository,
      sensorRepository: SensorRepository,
      jsonSender = jsonSender
    )
  )

  override def findOrCreateActuator(location: Location, name: String): Actuator =
    actuators.find(a => a.location == location && a.name == name).get

  override def find(location: Location, name: String): Option[Actuator] =
    actuators.find(a => a.location == location && a.name == name)

  override def findAll(): Seq[Actuator] = actuators
}
