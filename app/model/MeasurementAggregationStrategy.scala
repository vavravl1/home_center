package model

/**
  * Defines how measurements should be aggregated in the given period of time
  */
sealed trait MeasurementAggregationStrategy {

  /**
    * Transform single value
    */
  def singleValue(value: Double): Double
}

object NoneMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = value
}

object BooleanMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = if(value > 0) 10 else 0
}