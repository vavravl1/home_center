package model.ifthen

import model.sensor._
import mqtt.listener.SensorMeasurementsListener

/**
  * Class responsible for execution of if-thens based on mqtt messages
  */
class SensorMeasurementsIfThenExecutor(ifThens: Seq[IfThen]) extends SensorMeasurementsListener {
  override def messageReceived(
                       sensor: Sensor,
                       phenomenon: MeasuredPhenomenon,
                       measurement: Measurement
                     ) = {
    ifThens.foreach(ifThen => ifThen.action(sensor, phenomenon, measurement))
  }
}
