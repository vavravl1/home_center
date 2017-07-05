package model.actuator

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Writes}

/**
  * Represents value of a single command argument
  */
case class CommandArgument(val name: String, val unit: String, val value: String)

object CommandArgument {
  implicit val writes: Writes[CommandArgument] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "unit").write[String] and
      (JsPath \ "value").write[String]
    ) (unlift(CommandArgument.unapply))
}

