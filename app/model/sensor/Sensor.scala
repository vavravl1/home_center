package model.sensor

import _root_.play.api.libs.json._
import model.location.Location
import model.sensor.impl.SensorSql

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
    * When all measured phenomenons are single value aggregation, then this value is true. False otherwise.
    * When there are zero measured phenomenons, return false.
    *
    */
  def areAllMeasuredPhenomenonsSingleValue:Boolean

  /**
    * All measured phenomenons by this sensor
    */
  def measuredPhenomenons:Seq[MeasuredPhenomenon]

  /**
    * Create or load measured phenomenon according to the given parameters
    */
  def findOrCreatePhenomenon(name: String, unit:String, aggregationStrategy: MeasurementAggregationStrategy):MeasuredPhenomenon

  /**
    * Add single measurement that is associated with this sensor
    */
  def addMeasurement(measurement: Measurement, measuredPhenomenon:MeasuredPhenomenon)

  /**
    * Remove old un-aggregated measurements and replace them by aggregated one.
    * Aggregation is done by hours.
    */
  def aggregateOldMeasurements()

  override def equals(obj: scala.Any): Boolean = {
    if(!obj.isInstanceOf[Sensor]) {
      return false
    } else {
      val other = obj.asInstanceOf[Sensor]
      return other.name == this.name &&
        other.location == this.location
    }
  }
}

object Sensor {
  implicit val writes: Writes[Sensor] =
    new Writes[Sensor] {
      def writes(o: Sensor): JsValue = o match {
        case s: SensorSql => SensorSql.writes.writes(s)
      }
    }
}
