package model.actuator

import play.api.libs.json.Json

/**
  * Represents value of a single command argument
  */
case class CommandArgument(val name: String, val unit: String, val value: String)

object CommandArgument {
  implicit val format = Json.format[CommandArgument]
}

