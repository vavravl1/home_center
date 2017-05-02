package model


/**
  *
  */
trait SensorRepository {
  def findOrCreateSensor(locationAddress:String, name:String, measuresPhenomenon:String, unit:String): Sensor
  def find(locationAddress:String, measuredPhenomenon:String): Option[Sensor]
  def findAll(): Seq[Sensor]
}
