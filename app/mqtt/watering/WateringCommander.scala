package mqtt.watering

import entities.watering.WateringCommand
import mqtt.JsonSender
import play.api.Logger

import scala.concurrent.Future

/**
  * Sends messages to watering
  */
class WateringCommander(jsonSender: JsonSender) {
  def sendCommand(command: WateringCommand): Future[Unit] = {
    Logger.info(s"Sending watering command ${command}")
    jsonSender.send(
      "home/watering/ibisek/commands",
      command
    )(WateringCommand.comWrites)
  }
}
