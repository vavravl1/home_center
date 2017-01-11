package mqtt

import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

/**
  * Listens to the whole mqtt, translates messages to json a calls appropriate listeners
  */
class MqttDispatchingListener(var listeners: List[Listener]) extends MqttCallback {
  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}

  def addListener(listener:Listener): Unit = {
    listeners = listener :: listeners
  }

  override def messageArrived(topic: String, message: MqttMessage): Unit = {
    val msgStr = new String(message.getPayload)
    Logger.debug(s"Mqtt topic $topic received $msgStr")
    val json = Json.parse(msgStr)
    listeners
      .filter(l =>
        l.applies(topic)
      )
      .foreach(l =>
        l.messageReceived(topic, json)
      )
  }
}

/**
  * Listener for messages from mqtt
  */
abstract class Listener() {
  /**
    * Which topic the listener listens to
    */
  val topic: String

  /**
    * Determines if the message from the given topic should be processed
    * This determination is based on the name of the topic
    * @param receivedTopic topic that the message is coming from
    * @return true if the message should be processed, false otherwise
    */
  def applies(receivedTopic: String):Boolean = {
    if(topic.last == '#') {
      val prefix = topic.substring(0, topic.size - 1)
      receivedTopic.indexOf(prefix) == 0
    } else {
      receivedTopic.equals(topic)
    }
  }

  /**
    * Called to process received message by the listener
    * @param receivedTopic topic from which the message was received
    * @param json payload of the message
    */
  def messageReceived(receivedTopic: String, json: JsValue)
}

