package mqtt

import java.time.Clock

import akka.actor.{Actor, ActorSystem}
import config.HomeControllerConfiguration
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import org.eclipse.paho.client.mqttv3._
import play.api.Logger

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Replays every incoming mqtt message to remote mqtt broker
  */
class MqttRepeater(
                    remoteConfiguration: HomeControllerConfiguration,
                    actorSystem: ActorSystem,
                    localMqttConnector: MqttConnector,
                    clock: Clock
                  ) extends Actor {
  private val mqttConnector = new MqttConnector(
    remoteConfiguration,
    new RepeatingMqttCallback(localMqttConnector),
    actorSystem
  )

  override def receive(): Receive = {
    case Ping => mqttConnector.reconnect.run()
    case ConsumeMessage(receivedTopic: String, payload: String) =>
      Try(
        if(payload.contains(",")) {
          mqttConnector.send(receivedTopic, payload)
        } else {
          val payloadWithTs = payload + "," + clock.instant().getEpochSecond
          mqttConnector.send(receivedTopic, payloadWithTs)
        }
      ) match {
        case Success(_) => Logger.debug(s"Repeated $payload to $receivedTopic")
        case Failure(exception) => Logger.warn("Unable to repeat message", exception)
      }
  }
}


class RepeatingMqttCallback(localMqttConnector: MqttConnector) extends MqttCallback {

  override def messageArrived(receivedTopic: String, message: MqttMessage): Unit = receivedTopic match {
//    case MqttRepeater.commandTopic() => localMqttConnector.sendRaw(receivedTopic, new String(message.getPayload))
    case _ => Unit
  }

  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}
}
