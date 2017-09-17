package model.sensor

import dao.TimeGranularity
import model.sensor.impl.MeasuredPhenomenonSql
import play.api.libs.json.{JsValue, Writes}

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
  def measurements(timeGranularity: TimeGranularity): Seq[Measurement]

  /**
    * Remove old un-aggregated measurements and replace them by aggregated one.
    * Aggregation is done by hours.
    */
  def aggregateOldMeasurements()

  override def equals(obj: scala.Any): Boolean = {
    if(!obj.isInstanceOf[MeasuredPhenomenon]) {
      return false
    } else {
      val other = obj.asInstanceOf[MeasuredPhenomenon]
      return other.name == this.name &&
        other.unit == this.unit &&
        other.aggregationStrategy == this.aggregationStrategy
    }
  }
}

object MeasuredPhenomenon {
  def writesWithMeasurements(timeGranularity: TimeGranularity): Writes[Seq[MeasuredPhenomenon]] =
    new Writes[Seq[MeasuredPhenomenon]] {
      def writes(mps: Seq[MeasuredPhenomenon]): JsValue = mps match {
        case sql:Seq[MeasuredPhenomenonSql]  =>
          MeasuredPhenomenonSql.writesWithMeasurements(timeGranularity).writes(sql)
      }
    }
}
