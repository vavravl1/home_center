package model


/**
  * Repository for Sensor
  */
trait SensorRepository {
  /**
    * Find a sensor with the given locationAddress and measuredPhenomenon. If there is such
    * a sensor the other param will be ignored. And if there is no such sensor
    * a new one will be created according to the params.
    */
  def findOrCreateSensor(locationAddress:String, name:String, measuredPhenomenon:String, unit:String): Sensor

  /**
    * Find a sensor by its locationAddress and measuredPhenomenon.
    */
  def find(locationAddress:String, measuredPhenomenon:String): Option[Sensor]

  /**
    * Find all sensors ordered by their locationAddress
    */
  def findAll(): Seq[Sensor]

  /**
    * Delete given sensor
    */
  def delete(sensor: Sensor)
}
