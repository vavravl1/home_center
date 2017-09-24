package model.ifthen

import model.sensor.{MeasuredPhenomenon, Measurement}

/**
  *
  */
trait Condition {
  def apply(measuredPhenomenon: MeasuredPhenomenon, measurement:Measurement): Boolean
}

object AverageValueChanged extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    val possiblyLatMeasurement = phenomenon.lastNMeasurementsDescendant(2)
      .filter(_.measureTimestamp != measurement.measureTimestamp)
      .take(1)
      .filter(loaded => loaded.average == measurement.average)
    return possiblyLatMeasurement.isEmpty
  }
}

object TrueCondition extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = true
}
