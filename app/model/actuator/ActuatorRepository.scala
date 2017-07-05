package model.actuator

import model.Location

/**
  *
  */
trait ActuatorRepository {
  /**
    * Find an actuator with the given locationAddress and name. If there is no such actuator
    * a new one will be created according to the params.
    */
  def findOrCreateActuator(location:Location, name:String): Actuator

  /**
    * Find an actuator by its location and name.
    */
  def find(location:Location, name:String): Option[Actuator]

  /**
    * Find all sensors ordered by their locationAddress
    */
  def findAll(): Seq[Actuator]
}
