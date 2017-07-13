package model.actuator

import model.Location
import play.api.libs.json.{JsValue, Json, Writes}

/**
  * Represents actuator that can accept commands and do physical reactions
  */
trait Actuator {
  /**
    * name of the actuator, e.g. blind controller
    */
  val name: String

  /**
    *  location of this actuator
    */
  val location: Location

  /**
    * Returns all commands supported by this actuator
    */
  def supportedCommands:Set[Command]

  /**
    * Execute given command on this actuator. Only commands returned by supportedCommands
    * can be processed.
    *
    * @param command to be executed
    */
  def execute(command:Command): Unit
}

object Actuator {
  implicit val writes = new Writes[Actuator] {
    def writes(actuator: Actuator): JsValue = {
      Json.obj(
        "name" -> actuator.name,
        "location" -> Json.toJson(actuator.location)(Location.writes),
        "supportedCommands" -> Json.toJson(actuator.supportedCommands)
      )
    }
  }
}
