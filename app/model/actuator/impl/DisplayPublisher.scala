package model.actuator.impl

import model.actuator.{Actuator, Command, CommandArgument}
import model.location.Location
import mqtt.JsonSender
import play.api.Logger

/**
  *
  */
case class DisplayPublisher(
                             location: Location,
                             jsonSender: JsonSender
                           ) extends Actuator {
  override val name: String = "Display"
  override def supportedCommands: Set[Command] = Set(
    Command("Update", Seq(CommandArgument("phenomenon", "?", ""))),
    Command("Thermostat", Seq(CommandArgument("thermostat", "?", "20")))

  )
  override def execute(command: Command): Unit = {
    command.requiredArguments.foreach(arg => {
      Logger.debug(s"Updating display state:node/${location.address}/vv-display/-/${arg.name}/set => ${arg.value}")
      jsonSender.send(
        s"node/${location.address}/vv-display/-/${arg.name}/set",
        arg.value
      )
    })
  }
}
