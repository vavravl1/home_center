package mqtt.watering

import java.time.{Duration, Instant}

import play.api.libs.json._

/**
  * Represents command to manual watering
  */
class SingleCommand
case class WateringCommand(commands: Set[SingleCommand])

case class ManualWateringCommand() extends SingleCommand
case class TimeCommand(actualTime: Instant) extends SingleCommand
case class HumidityMeasuringDelay(delay:Duration) extends SingleCommand
case class HumidityMeasurePowerDelay(delay:Duration) extends SingleCommand
case class HumidityBaseline(baseline:Int) extends SingleCommand
case class HumidityMeasureBufferSize(bufferSize:Int) extends SingleCommand
case class WateringPause(pause:Duration) extends SingleCommand

object WateringCommand {

  implicit val duWrites = new Writes[Duration] {
    override def writes(d: Duration): JsValue =
      JsNumber(d.getSeconds)
  }

  implicit val inWrites = new Writes[Instant] {
    override def writes(in: Instant): JsValue =
      JsNumber(in.getEpochSecond)
  }

  implicit val comWrites = new Writes[WateringCommand] {
    def writes(command: WateringCommand) = Json.obj(
      "command" -> JsObject(command.commands.map {
        case c: ManualWateringCommand => ("manual-watering", JsBoolean(true))
        case c: TimeCommand => ("set-time", JsNumber(c.actualTime.getEpochSecond))
        case c: HumidityMeasuringDelay => ("set-humidity-measuring-delay", JsNumber(c.delay.toMillis))
        case c: HumidityMeasurePowerDelay => ("set-humidity-measure-power-delay", JsNumber(c.delay.toMillis))
        case c: HumidityBaseline => ("set-humidity-baseline", JsNumber(c.baseline))
        case c: HumidityMeasureBufferSize => ("set-humidity-measure-buffer-size", JsNumber(c.bufferSize))
        case c: WateringPause => ("set-watering-pause", JsNumber(c.pause.toMillis))
      }.toMap)
    )
  }

}
