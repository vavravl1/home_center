package model

import java.time.Instant

import play.api.libs.json.{Format, Json}

/**
  * Represents single measured value of the associated sensor in the given period of time
  *
  */
case class Measurement(
                 val average: Double,
                 val min: Double,
                 val max: Double,
                 val measureTimestamp: Instant
           )

object Measurement {
  implicit val format: Format[Measurement] = Json.format[Measurement]

  def apply(value:Double, measureTimestamp: Instant):Measurement =
    Measurement(value, value, value, measureTimestamp)
}