package model.sensor

import play.api.libs.json.{JsString, JsValue, Writes}

/**
  * Defines how measurements should be aggregated in the given period of time
  */
sealed trait MeasurementAggregationStrategy {

  /**
    * Transform single value
    */
  def singleValue(value: Double): Double
}

object IdentityMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = value
}

object BooleanMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = if(value > 0) 10 else 0
}

object MeasurementAggregationStrategy {
  implicit val writes: Writes[MeasurementAggregationStrategy] =
    new Writes[MeasurementAggregationStrategy] {
      def writes(o: MeasurementAggregationStrategy): JsValue = o match {
        case IdentityMeasurementAggregationStrategy => JsString("none")
        case BooleanMeasurementAggregationStrategy => JsString("boolean")
      }
    }
}