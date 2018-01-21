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
  private val lightRelay = new BcRelayActuator(
    location = locationRepository.findOrCreateLocation("836d19833c33"),
    sensorRepository: SensorRepository,
    jsonSender = jsonSender
  )
  private val thermostatRelay = new BcRelayActuator(
    location = locationRepository.findOrCreateLocation("836d19822676"),
    sensorRepository: SensorRepository,
    jsonSender = jsonSender
  )
  private val displayPublisher = new DisplayPublisher(
    location = locationRepository.findOrCreateLocation("836d19822676"),
    jsonSender = jsonSender
  )
  private val vvBlindsActuator = new VVBlindsActuator(
    location = locationRepository.findOrCreateLocation("836d19822676"),
    jsonSender = jsonSender
  )


  private val actuators = Seq(
    lightRelay,
    thermostatRelay,
    displayPublisher,
    vvBlindsActuator
  )

  def initialize: Unit = {
    lightRelay.initialize
    thermostatRelay.initialize
  }

  override def findOrCreateActuator(location: Location, name: String): Actuator =
    actuators.find(a => a.location == location && a.name == name).get

  override def find(location: Location, name: String): Option[Actuator] =
    actuators.find(a => a.location == location && a.name == name)

  override def findAll(): Seq[Actuator] = actuators
}
