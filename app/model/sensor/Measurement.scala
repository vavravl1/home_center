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
                        average: Any,
                        min: Any,
                        max: Any,
                        measureTimestamp: Instant
                      )
object Measurement {
  implicit val writes: Writes[Measurement] = new Writes[Measurement] {
    override def writes(o: Measurement): JsValue = o.average match {
        case num: Double =>
          Json.obj(
            "average" -> JsNumber(math.floor(num * 100) / 100),
            "min" -> JsNumber(math.floor(o.min.asInstanceOf[Double] * 100) / 100),
            "max" -> JsNumber(math.floor(o.max.asInstanceOf[Double] * 100) / 100),
            "measureTimestamp" -> Json.toJson(o.measureTimestamp)
          )
        case bool: Boolean =>
          Json.obj(
            "average" -> JsBoolean(bool),
            "min" -> JsBoolean(o.min.asInstanceOf[Boolean]),
            "max" -> JsBoolean(o.max.asInstanceOf[Boolean]),
            "measureTimestamp" -> Json.toJson(o.measureTimestamp)
          )
        case _ =>
          Json.obj(
            "average" -> JsString(o.average.toString),
            "min" -> JsString(o.min.toString),
            "max" -> JsString(o.max.toString),
            "measureTimestamp" -> Json.toJson(o.measureTimestamp)
          )
      }
  }

  implicit val reads: Reads[Measurement] = (
    (JsPath \ "average").read[Double] and
      (JsPath \ "min").read[Double] and
      (JsPath \ "max").read[Double] and
      (JsPath \ "measureTimestamp").read[Instant]
    ) (Measurement.createMeasurement _)

  def apply(value: Any, measureTimestamp: Instant): Measurement =
    Measurement(value, value, value, measureTimestamp)

  def createMeasurement(
                         average: Double,
                         min: Double,
                         max: Double,
                         measureTimestamp: Instant
                       ): Measurement =
    Measurement(average, min, max, measureTimestamp)

}