package entities.bigclown

import java.time.Instant

import play.api.libs.json._
import scalikejdbc._


/**
  * Represents measured value from any bc sensor
  *
  * @param location         where the value was taken, e.g. "remote/2"
  * @param sensor           type of the sensor that measured this value
  * @param phenomenon       of the measured value
  * @param measureTimestamp when the measure happened
  * @param value            value of the measure
  * @param unit             unit of the measure
  */
case class BcMeasure(
                      location: String,
                      sensor: String,
                      phenomenon: String,
                      measureTimestamp: Instant,
                      value: Double,
                      unit: String
                    )

object BcMeasure extends SQLSyntaxSupport[BcMeasure] {
  implicit val writes: Writes[BcMeasure] = Json.writes[BcMeasure]
}

/**
  * Represents aggregated measures over a given period of time.
  *
  * @param sensor           type of the sensor that measured this value
  * @param phenomenon       of the measured value
  * @param measureTimestamp when the measure happened
  * @param min              minimum value in the given period of time
  * @param max              maximum value in the given period of time
  * @param average          average value in the given period of time
  * @param unit             unit of the measure
  */
case class AggregatedBcMeasure(
                                sensor: String,
                                phenomenon: String,
                                measureTimestamp: Instant,
                                min: Double,
                                max: Double,
                                average: Double,
                                unit: String
                              )

object AggregatedBcMeasure extends SQLSyntaxSupport[AggregatedBcMeasure] {
  implicit val writes: Writes[AggregatedBcMeasure] = Json.writes[AggregatedBcMeasure]
}

/**
  * Represents message that is send by BigClown sensors
  *
  * @param phenomenon which is measured, e.g. temperature
  * @param value      value of the measure, e.g 19.24
  * @param unit       unit of the measure, e.g. lux
  */
case class BcMessage(phenomenon: String, value: Double, unit: String)

object BcMessage extends SQLSyntaxSupport[BcMeasure] {
  implicit val reads = new Reads[BcMessage] {
    override def reads(json: JsValue): JsResult[BcMessage] = {
      json match {
        case JsObject(map) => map.toList match {
          case List((phenomenon, JsArray(Seq(JsNumber(value), JsString(unit))))) =>
            JsSuccess(new BcMessage(phenomenon, value.doubleValue(), unit))
          case _ => JsError(error = s"Not a BcMeasure json: $json ")
        }
        case _ => JsError(error = s"Not a BcMeasure json: $json ")
      }
    }
  }
}



