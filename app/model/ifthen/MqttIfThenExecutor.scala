package model.ifthen

import model.sensor._
import mqtt.MqttListener

/**
  * Class responsible for execution of if-thens based on mqtt messages
  */
class MqttIfThenExecutor(ifThens: Seq[IfThen]) extends MqttListener {
  override def messageReceived(
                       sensor: Sensor,
                       phenomenon: MeasuredPhenomenon,
                       measurement: Measurement
                     ) = {
    ifThens.foreach(ifThen => ifThen.action(sensor, phenomenon, measurement))
  }
}
