package mqtt.repeater

import java.time.{Clock, Duration, Instant}

import akka.actor.{Actor, ActorPath, ActorSystem}
import mqtt.MqttConnector
import mqtt.repeater.MqttRepeaterMessage.RepeatMessage
import play.api.Logger

import scala.collection.mutable

/**
  *
  */
class MqttRepeater(
                           remoteMqttConnector: MqttConnector,
                           actorSystem: ActorSystem,
                           senders: Seq[ActorPath],
                           clock: Clock
                         ) extends Actor {
  val MINIMAL_DELAY = Duration.ofSeconds(5)

  var repeatedTopics = mutable.Map[String, Instant]()
  var actualSenderIndex = 0

  override def receive: Receive = {
    case MqttRepeaterMessage.Ping => ping
    case MqttRepeaterMessage.RepeatMessage(topic, message) => repeat(topic, message)
  }

  def repeat(topic: String, message: String): Unit = {
    val now = clock.instant
    val canSend = repeatedTopics.get(topic)
      .map(lastTime => lastTime.plus(MINIMAL_DELAY).isBefore(now))
      .getOrElse(true)
    if(canSend) {
      actorSystem.actorSelection(senders(actualSenderIndex)) ! RepeatMessage(topic, message)
      actualSenderIndex = (actualSenderIndex + 1) % senders.length
      repeatedTopics(topic) = now
      Logger.debug(s"Topic ${topic} is ready to be repeated")
    } else {
      val time = repeatedTopics.get(topic).get.getEpochSecond
      Logger.debug(s"Topic ${topic} is too early, remaining ${now.minusSeconds(time).plus(MINIMAL_DELAY).getEpochSecond} s")
    }
  }

  def ping: Unit = {
    Logger.info("Starting MqttRepeaterLimiter")
    remoteMqttConnector.reconnect.run()
    senders.foreach(actorSystem.actorSelection(_) ! MqttRepeaterMessage.Ping)
  }
}

object MqttRepeaterMessage {
  case class Ping()
  case class RepeatMessage(receivedTopic: String, message: String)
}
