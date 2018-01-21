package model.actuator.impl

import model.actuator.{Actuator, Command}
import model.location.Location
import mqtt.JsonSender
import play.api.Logger

/**
  * Represents VV Blind controller
  */
case class VVBlindsActuator(
                       location: Location,
                       jsonSender: JsonSender
                     ) extends Actuator {
  override val name: String = "Blinds"

  override def supportedCommands: Set[Command] = Set(
    Command("Up", Seq.empty),
    Command("Down", Seq.empty)
  )

  override def execute(command: Command): Unit = {
    if(command.name == "Up" || command.name == "Down") {
      Logger.info(s"VVBlindsActuator called and goes ${command.name}")
      jsonSender.send(
        s"node/${location.address}/vv-display/-/blinds/set",
        s""""${command.name.toLowerCase()}""""
      )
    }
  }
}
