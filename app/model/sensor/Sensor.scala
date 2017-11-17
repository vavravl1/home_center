package model.sensor

import _root_.play.api.libs.json._
import model.location.Location

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
    * Find measure phenomenon of this sensor
    */
  def findPhenomenon(name: String):Option[MeasuredPhenomenon]

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
  implicit val writes: Writes[Sensor] = new Writes[Sensor] {
    def writes(s: Sensor): JsValue = {
      Json.obj(
        "name" -> s.name,
        "location" -> Json.toJson(s.location)(Location.writes),
        "areAllMeasuredPhenomenonsSingleValue" -> Json.toJson(s.areAllMeasuredPhenomenonsSingleValue),
        "measuredPhenomenons" -> Json.toJson(s.measuredPhenomenons)(MeasuredPhenomenon.writes)
      )
    }
  }
}
