package model.ifthen

import model.sensor.Measurement

/**
  *
  */
trait Condition {
  def apply(measurements:Measurement): Boolean
}

object TrueCondition extends Condition {
  override def apply(measurements: Measurement): Boolean = true
}
