package mqtt

import akka.actor.{ActorPath, ActorSystem}
import model.sensor.{MeasuredPhenomenon, Measurement, Sensor}
import mqtt.MqttListenerMessage.ConsumeMessage
import mqtt.clown.MqttBigClownParser
import org.eclipse.paho.client.mqttv3.{IMqttDeliveryToken, MqttCallback, MqttMessage}
import play.api.Logger

/**
  * Listens to the whole mqtt and repeats it to all listeners
  */
class MqttDispatchingListener(
                               actorSystem: ActorSystem,
                               parser: MqttBigClownParser
                             ) extends MqttCallback {
  var listeners: List[ActorPath] = List.empty

  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}

  def addListener(listener:ActorPath): Unit = {
    listeners = listener :: listeners
  }

  override def messageArrived(topic: String, message: MqttMessage): Unit = {
    val msgStr = new String(message.getPayload)
    Logger.debug(s"Mqtt topic $topic received $msgStr")
    parser.parseMqttMessage(topic, msgStr).map({case (sensor, phenomenon, measurement) =>
      sensor.addMeasurement(measurement, phenomenon)
      listeners.foreach(actorSystem.actorSelection(_) ! ConsumeMessage(
        receivedTopic = topic,
        message = msgStr,
        sensor = sensor,
        phenomenon = phenomenon,
        measurement = measurement
      ))
    })

  }
}

object MqttListenerMessage {
  case class Ping()
  case class ConsumeMessage(
                             receivedTopic: String,
                             message: String,
                             sensor: Sensor,
                             phenomenon: MeasuredPhenomenon,
                             measurement: Measurement
                           )
}

