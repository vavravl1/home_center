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
    Logger.debug(s"Updating display state:node/${location.address}/vv-display/-/power/set => ${command.requiredArguments.head.value}")
    val displayDataType = command.requiredArguments.head.name
    jsonSender.send(
      s"node/${location.address}/vv-display/-/$displayDataType/set",
      command.requiredArguments.head.value
    )
  }

}
