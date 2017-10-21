package mqtt

import akka.actor.Actor
import model.sensor.{MeasuredPhenomenon, Measurement, Sensor}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}

/**
  * Class for processing mqtt messages using actors
  */
abstract class MqttListener extends Actor {
  override def receive(): Receive = {
    case Ping => ping
    case ConsumeMessage(
       receivedTopic: String,
       message: String,
       sensor: Sensor,
       phenomenon: MeasuredPhenomenon,
       measurement: Measurement
    ) =>
      messageReceived(receivedTopic, message)
      messageReceived(sensor, phenomenon, measurement)
    case _ =>
  }

  /**
    * Initial message when actor is instantiated. Received only once in lifetime
    */
  def ping = ()

  /**
    * Called after a message is received*
    */
  def messageReceived(receivedTopic: String, message: String):Unit = {}

  /**
    * Called after a message is received
    */
  def messageReceived(
                       sensor: Sensor,
                       phenomenon: MeasuredPhenomenon,
                       measurement: Measurement
                     ): Unit = {}
}
