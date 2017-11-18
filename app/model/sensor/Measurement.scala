package model.sensor

import java.time.Instant

import play.api.libs.json._

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
  implicit def writes: Writes[Measurement] = new Writes[Measurement] {
    override def writes(o: Measurement): JsValue = Json.obj(
      "average" -> JsNumber(math.floor(o.average * 100) / 100),
      "min" -> JsNumber(math.floor(o.min * 100) / 100),
      "max" -> JsNumber(math.floor(o.max * 100) / 100),
      "measureTimestamp" -> Json.toJson(o.measureTimestamp)
    )
  }

  def apply(value: Double, measureTimestamp: Instant): Measurement =
    Measurement(value, value, value, measureTimestamp)
}