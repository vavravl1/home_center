package model

import _root_.play.api.libs.json._
import model.impl.SensorSql

/**
  * Represents single sensor, e.g. Big Clown temperature tag.
  * Name and location values must be unique for each sensor, in other words they represents primary key
  */
trait Sensor {
  /**
    * name of the sensor, e.g. barometer
    */
  val name: String

  /**
    *  location of this sensor
    */
  val location: Location

  /**
    * All measured phenomenons by this sensor
    */
  def measuredPhenomenons:Seq[MeasuredPhenomenon]

  /**
    * Add single measurement that is associated with this sensor
    */
  def addMeasurement(measurement: Measurement, measuredPhenomenonName: String, unit:String)

  /**
    * Remove old un-aggregated measurements and replace them by aggregated one.
    * Aggregation is done by hours.
    */
  def aggregateOldMeasurements()
}

object Sensor {
  implicit val writes: Writes[Sensor] =
    new Writes[Sensor] {
      def writes(o: Sensor): JsValue = o match {
        case s: SensorSql => SensorSql.writes.writes(s)
      }
    }
}
