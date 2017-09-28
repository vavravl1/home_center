package mqtt

import akka.actor.Actor
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}

/**
  * Class for processing mqtt messages using actors
  */
abstract class MqttListener extends Actor {
  override def receive(): Receive = {
    case Ping => ping
    case ConsumeMessage(receivedTopic: String, message: String) => messageReceived(receivedTopic, message)
    case _ =>
  }

  /**
    * Initial message when actor is instantiated. Received only once in lifetime
    */
  def ping = ()

  /**
    * Called after a message is received
    *
    * @param receivedTopic mqtt topic
    * @param message received message from mqtt without any change
    */
  def messageReceived(receivedTopic: String, message: String)
}
