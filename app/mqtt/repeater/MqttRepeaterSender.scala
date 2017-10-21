package mqtt.repeater

import java.time.Clock

import akka.actor.Actor
import mqtt.MqttConnector
import play.api.Logger

import scala.util.{Failure, Success, Try}

/**
  *
  */
class MqttRepeaterSender(
                          remoteMqttConnector: MqttConnector,
                          clock: Clock
                        ) extends Actor {
  override def receive(): Receive = {
    case MqttRepeaterMessage.Ping =>
    case MqttRepeaterMessage.RepeatMessage(receivedTopic: String, message: String) =>Try(
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
    case _ =>
  }
}
