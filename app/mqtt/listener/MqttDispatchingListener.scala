package mqtt.listener

import akka.actor.ActorRef
import mqtt.repeater.MqttRepeaterMessage
import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}
import play.api.Logger

/**
  * Listens to the whole mqtt and repeats it to SensorMeasurementsDispatcher and MqttRepeater
  */
class MqttDispatchingListener(
                               sensorMeasurementsDispatcher: ActorRef,
                               mqttRepeater: Option[ActorRef]
                             ) extends MqttCallback {

  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}

  override def messageArrived(topic: String, message: MqttMessage): Unit = {
    val msgStr = new String(message.getPayload)
    Logger.debug(s"Mqtt topic $topic received $msgStr")
    sensorMeasurementsDispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived(topic, msgStr)
    mqttRepeater.map(
      _ ! MqttRepeaterMessage.RepeatMessage(topic, msgStr)
    )
  }
}

