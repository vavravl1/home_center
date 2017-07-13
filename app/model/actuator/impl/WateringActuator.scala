package model.actuator.impl

import model.actuator.{Actuator, Command, CommandArgument}
import model.{Location, LocationRepository}
import mqtt.JsonSender
import play.api.Logger
import play.api.libs.json._

import scala.collection.immutable.Stream.Empty

/**
  *
  */
class WateringActuator(
                        locationRepository: LocationRepository,
                        jsonSender: JsonSender
                      ) extends Actuator {
  override val name: String = "Watering"
  override val location: Location = locationRepository.findOrCreateLocation("watering-ibiscus")

  override def supportedCommands: Set[Command] = Set(
    Command("manual watering", Empty),
    Command("clock time", Seq(CommandArgument("time", "epoch in seconds", "NOW"))),
    Command("delay between humidity measurements", Seq(CommandArgument("delay", "milli seconds", "5000"))),
    Command("delay between measuring power on and reading value", Seq(CommandArgument("delay", "milli seconds", "100"))),
    Command("humidity baseline for watering", Seq(CommandArgument("baseline", "humidity", "150"))),
    Command("humidity buffer size", Seq(CommandArgument("buffer size", "number of numbers", "13"))),
    Command("minimal pause between watering", Seq(CommandArgument("pause", "minutes", "15")))
  )

  override def execute(command: Command): Unit = {
    Logger.info(s"Sending watering command ${command}")

    val arguments = command.requiredArguments.map(arg => arg.value)

    val commandToSend = Json.obj(
      "command" -> JsObject(command.name match {
        case "manual watering" => Map("manual-watering" -> JsBoolean(true))
        case "clock time" => Map("set-time" -> JsNumber(Integer.parseInt(arguments.head)))
        case "delay between humidity measurements" => Map("set-humidity-measuring-delay" -> JsNumber(Integer.parseInt(arguments.head)))
        case "delay between measuring power on and reading value" => Map("set-humidity-measure-power-delay" -> JsNumber(Integer.parseInt(arguments.head)))
        case "humidity baseline for watering" => Map("set-humidity-baseline" -> JsNumber(Integer.parseInt(arguments.head)))
        case "humidity buffer size" => Map("set-humidity-measure-buffer-size" -> JsNumber(Integer.parseInt(arguments.head)))
        case "minimal pause between watering" => Map("set-watering-pause" -> JsNumber(Integer.parseInt(arguments.head) * 60 * 60 * 1000))
      })
    )

    jsonSender.send(
      "home/watering/ibisek/commands",
      commandToSend
    )
  }
}
