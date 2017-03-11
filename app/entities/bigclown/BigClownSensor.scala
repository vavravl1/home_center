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
object BcMeasure {
  implicit val writes: Writes[BcMeasure] = Json.writes[BcMeasure]
}

/**
  * Represents location of all associated sensors.
  * @param location as seen by BigClown nodes, e.g. "remote/2"
  * @param label as seen by the user, e.g. "Kitchen"
  */
case class BcSensorLocation(location:String, label: String)
object BcSensorLocation {
  implicit val format: Format[BcSensorLocation] = Json.format[BcSensorLocation]
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
                                location: String,
                                sensor: String,
                                phenomenon: String,
                                measureTimestamp: Instant,
                                min: Double,
                                max: Double,
                                average: Double,
                                unit: String
                              )

object AggregatedBcMeasure {
  implicit val writes: Writes[AggregatedBcMeasure] = Json.writes[AggregatedBcMeasure]
}

/**
  * Represents message that is send by BigClown sensors
  *
  * @param phenomenon which is measured, e.g. temperature
  * @param value      value of the measure, e.g 19.24
  * @param unit       unit of the measure, e.g. lux
  */
case class SingleBcMessageValue(phenomenon: String, value: Double, unit: String)

/**
  * Single bc sensor can produce multiple measures, e.g. barometer does that.
  * This represents all values from a single sensor.
  */
case class BcMessage(msgs: Seq[SingleBcMessageValue])

object BcMessage {
  implicit val reads = new Reads[BcMessage] {
    override def reads(json: JsValue): JsResult[BcMessage] = try {
      json match {
        case JsObject(measures) =>
          val msgs = measures.toList.map { case (phenomenon: String, value: JsValue) => value match {
            case JsArray (Seq (JsNumber (value), JsString (unit) ) ) =>
              SingleBcMessageValue (phenomenon, value.doubleValue (), unit)
            case _ => throw new RuntimeException(s"Not a BcMeasure json: $json ")
          }}
          JsSuccess(BcMessage(msgs))
        case _ => throw new RuntimeException(s"Not a BcMeasure json: $json ")
      }
    } catch {
      case e:RuntimeException =>
        JsError(s"Not a BcMeasure json: $json ")
    }
  }
}



