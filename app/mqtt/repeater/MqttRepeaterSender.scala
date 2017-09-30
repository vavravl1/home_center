package mqtt.repeater

import java.time.Clock

import mqtt.{MqttConnector, MqttListener}
import play.api.Logger

import scala.util.{Failure, Success, Try}

/**
  *
  */
class MqttRepeaterSender(
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
}
