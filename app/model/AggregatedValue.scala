package model

import java.time.Instant

import play.api.libs.json.{Format, Json}

/**
  * Represents aggregated value for a period of time
  *
  * @param min
  * @param max
  * @param average
  */
case class AggregatedValue(
                            val min:Double,
                            val max:Double,
                            val average:Double,
                            val measureTimestamp:Instant
                          )
object AggregatedValue {
  implicit val format: Format[AggregatedValue] = Json.format[AggregatedValue]
}