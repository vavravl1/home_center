package mqtt

import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.ActorSystem
import config.HomeControllerConfiguration
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

/**
  * Connector to the mqtt broker
  */
class MqttConnector(
                     configuration: HomeControllerConfiguration,
                     mqttListener: MqttCallback,
                     actorSystem: ActorSystem
                   ) extends JsonSender {
  private var mqttClient: Option[MqttClient] = None

  val reconnect: Runnable = {
    new Runnable {
      override def run(): Unit = {
        mqttClient match {
          case Some(_) => Logger.info("Mqtt client has connected")
          case None if configuration.mqttClientId != null =>
            mqttClient = connect()
            actorSystem.scheduler.scheduleOnce(10 seconds, reconnect)
          case None if configuration.mqttClientId == null => Unit
        }
      }
    }
  }

  private def connect(): Option[MqttClient] = try {
    Logger.info(s"Connecting to broker $configuration.mqttBrokerUrl as $configuration.mqttClientId")

    val mqttClient = new MqttClient(configuration.mqttBrokerUrl, configuration.mqttClientId, new MemoryPersistence)
    val connOpts = new MqttConnectOptions()
    connOpts.setAutomaticReconnect(true)
    connOpts.setCleanSession(true)
    mqttClient.setCallback(mqttListener)
    mqttClient.setTimeToWait(5000)
    mqttClient.connect(connOpts)
    mqttClient.subscribe("#")
    Logger.info("Connected")
    return Some(mqttClient)
  } catch {
    case me: MqttException =>
      Logger.error("Connection failed due {}", me)
      return None
  }

  def disconnect(): Future[Unit] = {
    Future {
      Logger.info("Disconnecting from mqtt and waiting for all messages to be processed")
      mqttClient match {
        case Some(client) => client.disconnect();
        case None =>
      }
      Logger.info("Mqtt is disconnected")
    }
  }

  override def send[T](topic: String, payload: T)(implicit writes: Writes[T]): Future[Unit] = {
    Future {
      mqttClient match {
        case Some(client) => Logger.debug(s"Publishing ${Json.toJson(payload)(writes).toString()} to $topic")
          client.publish(
            topic,
            new MqttMessage(Json.toJson(payload)(writes).toString().getBytes)
          )
        case None => throw new RuntimeException("Mqtt client is not yet connected")
      }
    }
  }

  override def sendRaw(topic: String, payload: String): Unit = {
      mqttClient match {
        case Some(client) => Logger.debug(s"Raw publishing $payload to $topic")
          client.publish(
            topic,
            new MqttMessage(payload.getBytes)
          )
        case None => {}
    }
  }
}
