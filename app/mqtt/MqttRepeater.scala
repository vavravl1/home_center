package mqtt

import akka.actor.{Actor, ActorSystem}
import config.HomeControllerConfiguration
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import org.eclipse.paho.client.mqttv3._
import play.api.Logger
import play.api.libs.json.JsValue

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Replays every incoming mqtt message to remove mqtt broker
  */
class MqttRepeater(
                    remoteConfiguration: HomeControllerConfiguration,
                    actorSystem: ActorSystem,
                    localMqttConnector: MqttConnector
                  ) extends Actor {
  private val mqttConnector = new MqttConnector(
    remoteConfiguration,
    new RepeatingMqttCallback(localMqttConnector),
    actorSystem
  )

  override def receive(): Receive = {
    case Ping => mqttConnector.reconnect.run()
    case ConsumeMessage(receivedTopic: String, json: JsValue) => receivedTopic match {
      case MqttRepeater.commandTopic() => Unit // Don't replay watering commands
      case _ => Try(mqttConnector.sendRaw(receivedTopic, json.toString())) match {
        case Success(_) =>
        case Failure(exception) => Logger.warn("Unable to repeat message", exception)
      }

    }
  }
}

object MqttRepeater {
  val commandTopic = "home/watering/ibisek/commands".r
}

class RepeatingMqttCallback(localMqttConnector: MqttConnector) extends MqttCallback {

  override def messageArrived(receivedTopic: String, message: MqttMessage): Unit = receivedTopic match {
    case MqttRepeater.commandTopic() => localMqttConnector.sendRaw(receivedTopic, new String(message.getPayload))
    case _ => Unit
  }

  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}
}
