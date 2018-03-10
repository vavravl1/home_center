package model.ifthen

import java.time.temporal.ChronoUnit
import java.time.{Clock, Duration, Instant}

import dao.ByHour
import model.sensor.{MeasuredPhenomenon, Measurement}

import scala.concurrent.Await

/**
  *
  */
trait Condition {
  def apply(measuredPhenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean
}

case class DelayedCondition(clock: Clock, delay: Duration) extends Condition {
  var lastTimeEvaluated = clock.instant()

  override def apply(measuredPhenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    if (clock.instant().isAfter(lastTimeEvaluated.plus(delay))) {
      lastTimeEvaluated = clock.instant()
      true
    } else {
      false
    }
  }
}

object AverageValueChanged extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    val lastTwo = phenomenon.lastNMeasurementsDescendant(2)
    return lastTwo.size >= 2 && lastTwo(0).average != lastTwo(1).average
  }
}

object TrueCondition extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = true
}

case class AndCondition(left: Condition, right: Condition) extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    left(phenomenon, measurement) && right(phenomenon, measurement)
  }
}

case class LowerThan(value: Double) extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    measurement.average < value
  }
}

case class GreaterThan(value: Double) extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    measurement.average > value
  }
}

case class NoMeasurementsInLast4Hours(watched: MeasuredPhenomenon, clock: Clock) extends Condition {
  override def apply(phenomenon: MeasuredPhenomenon, measurement: Measurement): Boolean = {
    val now = clock.instant()
    val measurements:Seq[Measurement] = Await.result(
      watched.measurements(ByHour),
      scala.concurrent.duration.Duration.Inf
    )
    !isThereEarlyOne(measurements, now)
  }

  private def isThereEarlyOne(measurements:Seq[Measurement], now: Instant) = {
    measurements.exists(_.measureTimestamp.plus(4, ChronoUnit.HOURS).isAfter(now))
  }

}
