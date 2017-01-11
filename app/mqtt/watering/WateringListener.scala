package mqtt.watering

import dao.WateringDao
import entities.watering.WateringMessage
import mqtt.Listener
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from watering to dao
  */
class WateringListener(dao:WateringDao) extends Listener {
  override val topic = "home/watering/ibisek/telemetry"

  override def messageReceived(receivedTopic: String, json: JsValue):Unit = {
    val fromJson = Json.fromJson(json)(WateringMessage.wmReads)
    fromJson match {
      case JsSuccess(value, _) => dao.save(value)
      case JsError(_) => Logger.error(s"Parsing $json failed");
    }
  }
}
