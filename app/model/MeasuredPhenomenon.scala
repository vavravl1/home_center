package model

import dao.TimeGranularity

/**
  * Represents one measured phenomenon, e.g. temperature.
  * This measured phenomenon always belongs to one and only one sensor
  */
trait MeasuredPhenomenon {
  /**
    * Name of the measured phenomenon, e.g. temperature
    */
  val name:String

  /**
    * Unit of the measured phenomenon, e.g. Celsius
    */
  val unit:String

  /**
    * Returns all measurements aggregated by the given time granularity
    */
  def measurements(timeGranularity: TimeGranularity):Seq[Measurement]

  /**
    * Remove old un-aggregated measurements and replace them by aggregated one.
    * Aggregation is done by hours.
    */
  def aggregateOldMeasurements()
}
