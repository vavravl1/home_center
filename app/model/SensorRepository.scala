package model


/**
  * Repository for Sensor
  */
trait SensorRepository {
  /**
    * Find a sensor with the given locationAddress and name. If there is no such sensor
    * a new one will be created according to the params.
    */
  def findOrCreateSensor(location:Location, name:String): Sensor

  /**
    * Find a sensor by its location and name.
    */
  def find(location:Location, name:String): Option[Sensor]

  /**
    * Find all sensors ordered by their locationAddress
    */
  def findAll(): Seq[Sensor]

  /**
    * Delete given sensor
    */
  def delete(sensor: Sensor)
}
