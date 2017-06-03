package mqtt.watering

import java.time.temporal.ChronoUnit
import java.time.{Clock, Duration}

import akka.actor.Actor
import entities.watering._
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger

/**
  * Listens for hello messages and sets default settings of watering
  */
class WateringHelloListener(wateringCommander: WateringCommander, clock: Clock) extends Actor {
  val topic = "home/watering/ibisek/hello".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, _: String) => receivedTopic match {
      case topic() =>
        Logger.info("Ibisek watering says hello")
        val wc = WateringCommand(Set(
          TimeCommand(clock.instant()),
          HumidityMeasuringDelay(Duration.of(5, ChronoUnit.SECONDS)),
          HumidityMeasurePowerDelay(Duration.of(100, ChronoUnit.MILLIS)),
          HumidityBaseline(200),
          HumidityMeasureBufferSize(13),
          WateringPause(Duration.of(30, ChronoUnit.MINUTES))
        ))
        wateringCommander.sendCommand(wc)
      case _ => {}
    }
  }
}
