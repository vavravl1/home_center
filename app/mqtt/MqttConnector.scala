package mqtt

import scala.concurrent.duration._

import akka.actor.ActorSystem
import config.HomeControllerConfiguration
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions, MqttException, MqttMessage}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

/**
  * Connector to the mqtt broker
  */
class MqttConnector(
                     configuration: HomeControllerConfiguration,
                     val mqttListener: MqttDispatchingListener,
                     val actorSystem: ActorSystem
                   ) extends JsonSender {
  private var mqttClient: Option[MqttClient] = None
  actorSystem.scheduler.scheduleOnce(0 seconds, reconnect)

  def reconnect():Runnable = {
    new Runnable {
      override def run(): Unit = {
        mqttClient match {
          case Some(_) => Logger.info("Mqtt client has connected")
          case None =>
            mqttClient = connect()
            actorSystem.scheduler.scheduleOnce(2 seconds, reconnect)
        }
      }
    }
  }

  private def connect(): Option[MqttClient] = try {
    val mqttClient = new MqttClient(configuration.mqttBrokerUrl, configuration.mqttClientId, new MemoryPersistence)
    val connOpts = new MqttConnectOptions()
    connOpts.setAutomaticReconnect(true)
    connOpts.setCleanSession(true)
    Logger.info(s"Connecting to broker $configuration.mqttBrokerUrl as $configuration.mqttClientId")
    mqttClient.connect(connOpts)
    Logger.info("Connected")
    mqttClient.setCallback(mqttListener)
    mqttClient.subscribe("#")
    mqttClient.setTimeToWait(300)
    Some(mqttClient)
  } catch {
    case me: MqttException =>
      Logger.error("Connection failed due {}", me)
      None
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
}
