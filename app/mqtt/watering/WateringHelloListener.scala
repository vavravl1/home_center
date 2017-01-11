package mqtt.watering

import java.time.temporal.ChronoUnit
import java.time.{Clock, Duration}

import entities.watering._
import mqtt.Listener
import play.api.Logger
import play.api.libs.json.JsValue

/**
  * Listens for hello messages and sets default settings of watering
  */
class WateringHelloListener(wateringCommander: WateringCommander, clock: Clock) extends Listener {
  override val topic: String = "home/watering/ibisek/hello"

  override def messageReceived(receivedTopic: String, json: JsValue): Unit = {
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
  }
}
