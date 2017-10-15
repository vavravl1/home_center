package model.ifthen

import java.time.{Clock, Duration}

import model.sensor.{MeasuredPhenomenon, Measurement}

/**
  *
  */
trait Condition {
  def apply(measuredPhenomenon: MeasuredPhenomenon, measurement:Measurement): Boolean
}

case class DelayedCondition(clock: Clock, delay: Duration) extends Condition {
  var lastTimeEvaluated = clock.instant()
  override def apply(measuredPhenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    if(clock.instant().isAfter(lastTimeEvaluated.plus(delay))) {
      lastTimeEvaluated = clock.instant()
      true
    } else {
      false
    }
  }
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
