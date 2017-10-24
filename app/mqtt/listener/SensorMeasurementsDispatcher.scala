package mqtt.listener

import akka.actor.{Actor, ActorRef, ActorSystem}
import mqtt.clown.MqttBigClownParser
import play.api.Logger

/**
  * Takes message passed by Akka directly from mqtt listener,
  * stores it and then dispatches it to all listeners
  */
class SensorMeasurementsDispatcher(actorSystem: ActorSystem,
                                   parser: MqttBigClownParser,
                                   listeners: Seq[ActorRef]
                              ) extends Actor {
  override def receive: Receive = {
    case SensorMeasurementsDispatcherMessages.Ping => listeners.foreach(_ ! SensorMeasurementsListenerMessages.Ping)
    case SensorMeasurementsDispatcherMessages.MessageReceived(topic, message) =>
      Logger.debug(s"Mqtt topic $topic received $message")
      parser.parseMqttMessage(topic, message).map({ case (sensor, phenomenon, measurement) =>
        sensor.addMeasurement(measurement, phenomenon)
        listeners.foreach(_ ! SensorMeasurementsListenerMessages.ConsumeMessage(
          sensor = sensor,
          phenomenon = phenomenon,
          measurement = measurement
        ))
      })
  }
}

object SensorMeasurementsDispatcherMessages {
  case class Ping()
  case class MessageReceived(topic: String, message: String)
}