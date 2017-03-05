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
                    actorSystem: ActorSystem
                  ) extends Actor {
  private val mqttConnector = new MqttConnector(
    remoteConfiguration,
    new RepeatingMqttCallback(),
    actorSystem
  )

  override def receive(): Receive = {
    case Ping => mqttConnector.reconnect.run()
    case ConsumeMessage(receivedTopic: String, json: JsValue) =>
      Try(mqttConnector.sendRaw(receivedTopic, json.toString())) match {
        case Success(_) =>
        case Failure(exception) => Logger.warn("Unable to repeat message", exception)
      }
  }
}

class RepeatingMqttCallback extends MqttCallback {
  override def deliveryComplete(token: IMqttDeliveryToken): Unit = {}
  override def messageArrived(topic: String, message: MqttMessage): Unit = {}
  override def connectionLost(cause: Throwable): Unit = {}
}
