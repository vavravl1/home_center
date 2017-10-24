package mqtt.repeater

import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}

object EmptyMqttCallback extends MqttCallback {
  override def messageArrived(receivedTopic: String, message: MqttMessage): Unit = {}
  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}
}
