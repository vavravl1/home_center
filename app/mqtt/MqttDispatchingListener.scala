package mqtt

import akka.actor.{ActorPath, ActorSystem}
import mqtt.MqttListenerMessage.ConsumeMessage
import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}
import play.api.Logger

/**
  * Listens to the whole mqtt and repeats it to all listeners
  */
class MqttDispatchingListener(actorSystem: ActorSystem) extends MqttCallback {
  var listeners: List[ActorPath] = List.empty

  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}

  def addListener(listener:ActorPath): Unit = {
    listeners = listener :: listeners
  }

  override def messageArrived(topic: String, message: MqttMessage): Unit = {
    val msgStr = new String(message.getPayload)
    Logger.debug(s"Mqtt topic $topic received $msgStr")

    listeners.foreach(actorSystem.actorSelection(_) ! ConsumeMessage(topic, msgStr))
  }
}

object MqttListenerMessage {
  case class Ping()
  case class ConsumeMessage(receivedTopic: String, message: String)
}

