package mqtt.watering

import java.time.{Duration, Instant}

import entities.CommonJsonReadWrite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Represents state of one watering station in particular point in time
  */
case class WateringMessage(timestamp: Instant, telemetry: WateringTelemetry)
case class WateringTelemetry(humidity: Humidity, watering: Watering, waterLevelHigh: Boolean)
case class Humidity(actual: Int, baseLine: Int, measuringDelay: Duration, bufferSize: Int, powerDelay: Duration)
case class Watering(inProgress: Boolean, wateringPause: Duration, wateringPumpTime: Duration)

object WateringMessage extends CommonJsonReadWrite {

  def zero(): WateringMessage = {
    WateringMessage(Instant.EPOCH,
      WateringTelemetry(
        Humidity(0, 0, Duration.ofSeconds(0), 0, Duration.ofSeconds(0)),
        Watering(false, Duration.ofSeconds(0), Duration.ofSeconds(0)),
        false
      )
    )
  }

  implicit val huWrites = Json.writes[Humidity]
  implicit val watWrites = Json.writes[Watering]
  implicit val wtWrites = Json.writes[WateringTelemetry]
  implicit val wmWrites: Writes[WateringMessage] = Json.writes[WateringMessage]

  implicit val huReads: Reads[Humidity] = (
    (JsPath \ "a").read[Int] and
      (JsPath \ "bl").read[Int] and
      (JsPath \ "md").read[Duration] and
      (JsPath \ "bs").read[Int] and
      (JsPath \ "pd").read[Duration]
    ) (Humidity.apply _)

  implicit val watReads: Reads[Watering] = (
    (JsPath \ "ip").read[Boolean] and
      (JsPath \ "wp").read[Duration] and
      (JsPath \ "wt").read[Duration]
    ) (Watering.apply _)

  implicit val wtReads: Reads[WateringTelemetry] = (
    (JsPath \ "hu").read[Humidity] and
      (JsPath \ "wa").read[Watering] and
      (JsPath \ "wlh").read[Boolean]
    ) (WateringTelemetry.apply _)

  implicit val wmReads: Reads[WateringMessage] = (
    (JsPath \ "ts").read[Instant](instantInSecondsReads) and
      (JsPath \ "tm").read[WateringTelemetry]
    ) (WateringMessage.apply _)
}