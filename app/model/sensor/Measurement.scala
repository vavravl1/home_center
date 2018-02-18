package model.sensor

import java.time.Instant

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

/**
  * Represents single measured value of the associated sensor in the given period of time
  *
  */
case class Measurement(
                        average: Double,
                        min: Double,
                        max: Double,
                        measureTimestamp: Instant
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

  implicit val reads: Reads[Measurement] = (
    (JsPath \ "average").read[Double] and
      (JsPath \ "min").read[Double] and
      (JsPath \ "max").read[Double] and
      (JsPath \ "measureTimestamp").read[Instant]
    ) (Measurement.createMeasurement _)

  def apply(value: Double, measureTimestamp: Instant): Measurement =
    Measurement(value, value, value, measureTimestamp)

  def createMeasurement(
                         average: Double,
                         min: Double,
                         max: Double,
                         measureTimestamp: Instant
                       ): Measurement =
    Measurement(average, min, max, measureTimestamp)

}