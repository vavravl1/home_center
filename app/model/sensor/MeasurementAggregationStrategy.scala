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

object SingleValueAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = value
}

object DoubleValuesMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = value
}

object EnumeratedMeasurementAggregationStrategy extends MeasurementAggregationStrategy {
  override def singleValue(value: Double): Double = if(value > 0) 10 else 0
}

object MeasurementAggregationStrategy {
  implicit val writes: Writes[MeasurementAggregationStrategy] =
    new Writes[MeasurementAggregationStrategy] {
      def writes(o: MeasurementAggregationStrategy): JsValue = o match {
        case DoubleValuesMeasurementAggregationStrategy => JsString("none")
        case SingleValueAggregationStrategy => JsString("singleValue")
        case EnumeratedMeasurementAggregationStrategy => JsString("boolean")
      }
    }
}