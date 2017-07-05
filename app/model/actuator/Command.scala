package model.actuator

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Writes}

/**
  * Represents single command that can be send to an actuator
  */
case class Command(val name:String, val requiredArguments:Seq[CommandArgument])

object Command {
  implicit val writes: Writes[Command] = (
      (JsPath \ "name").write[String] and
      (JsPath \ "requiredArguments").write[Seq[CommandArgument]]
    )(unlift(Command.unapply))
}


