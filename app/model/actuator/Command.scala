package model.actuator

import play.api.libs.json.Json

/**
  * Represents single command that can be send to an actuator
  */
case class Command(val name:String, val requiredArguments:Seq[CommandArgument])

object Command {
  implicit val format = Json.format[Command]
}


