package mqtt

import java.time.Clock

import akka.actor.ActorSystem
import org.eclipse.paho.client.mqttv3._
import play.api.Logger

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Replays every incoming mqtt message to remote mqtt broker
  */
class MqttRepeater(
                    actorSystem: ActorSystem,
                    localMqttConnector: MqttConnector,
                    remoteMqttConnector: MqttConnector,
                    clock: Clock
                  ) extends MqttListener {
  override def messageReceived(receivedTopic: String, message: String): Unit = Try(
    if(message.contains(",")) {
      remoteMqttConnector.send(receivedTopic, message)
    } else {
      val payloadWithTs = message + "," + clock.instant().getEpochSecond
      remoteMqttConnector.send(receivedTopic, payloadWithTs)
    }
  ) match {
    case Success(_) => Logger.debug(s"Repeated $message to $receivedTopic")
    case Failure(exception) => Logger.warn("Unable to repeat message", exception)
  }

  override def ping = remoteMqttConnector.reconnect.run()
}


class RepeatingMqttCallback(localMqttConnector: MqttConnector) extends MqttCallback {
  override def messageArrived(receivedTopic: String, message: MqttMessage): Unit = {}
  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}
}
