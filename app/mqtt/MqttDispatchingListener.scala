package mqtt

import akka.actor.{Actor, ActorPath, ActorSystem}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

/**
  * Listens to the whole mqtt, translates messages to json a calls appropriate listeners
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
    val json = Json.parse(msgStr)
    listeners.foreach(l =>
        actorSystem.actorSelection(l) ! ConsumeMessage(topic, json)
      )
  }
}

object MqttListenerMessage {
  case class Ping()
  case class ConsumeMessage(receivedTopic: String, json: JsValue)
}

