package mqtt.clown

import play.api.libs.json._

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

