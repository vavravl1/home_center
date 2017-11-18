package model.sensor

import dao.TimeGranularity
import play.api.libs.json.{JsArray, JsValue, Json, Writes}

import scala.concurrent.Future

/**
  * Represents one measured phenomenon, e.g. temperature.
  * This measured phenomenon always belongs to one and only one sensor
  */
trait MeasuredPhenomenon {
  /**
    * Name of the measured phenomenon, e.g. temperature
    */
  val name: String

  /**
    * Unit of the measured phenomenon, e.g. Celsius
    */
  val unit: String

  /**
    * Describes how the measured values should be aggregated
    */
  val aggregationStrategy: MeasurementAggregationStrategy

  /**
    * Returns all measurements aggregated by the given time granularity
    */
  def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]]

  /**
    * Returns last n measurements sorted by measured time descendant
    */
  def lastNMeasurementsDescendant(n: Int): Seq[Measurement]

  /**
    * Add single measurement
    */
  def addMeasurement(measurement: Measurement)

  override def equals(obj: scala.Any): Boolean = {
    if (!obj.isInstanceOf[MeasuredPhenomenon]) {
      false
    } else {
      val other = obj.asInstanceOf[MeasuredPhenomenon]
      other.name == this.name &&
        other.unit == this.unit &&
        other.aggregationStrategy == this.aggregationStrategy
    }
  }
}

object MeasuredPhenomenon {
  def writes: Writes[Seq[MeasuredPhenomenon]] = new Writes[Seq[MeasuredPhenomenon]] {
    def writes(mp: Seq[MeasuredPhenomenon]): JsValue =
      JsArray(mp.map(mp => Json.obj(
        "name" -> mp.name,
        "unit" -> mp.unit
      )))
  }
}
