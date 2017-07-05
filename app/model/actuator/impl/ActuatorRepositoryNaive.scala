package model.actuator.impl

import model.actuator.{Actuator, ActuatorRepository}
import model.{Location, LocationRepository}
import mqtt.JsonSender

/**
  *
  */
class ActuatorRepositoryNaive(
                               locationRepository: LocationRepository,
                               jsonSender: JsonSender
                             ) extends ActuatorRepository {

  val wateringActuator = new WateringActuator(
    locationRepository = locationRepository,
    jsonSender = jsonSender
  )

  override def findOrCreateActuator(location: Location, name: String): Actuator = wateringActuator

  override def find(location: Location, name: String): Option[Actuator] = Some(wateringActuator)

  override def findAll(): Seq[Actuator] = Seq(wateringActuator)
}
