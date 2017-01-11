package mqtt.clown

import java.time.Clock

import dao.BcMeasureDao
import entities.bigclown.{BcMeasure, BcMessage}
import mqtt.Listener
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from watering to dao
  */
class BridgeListener(bcDao: BcMeasureDao, clock: Clock) extends Listener {
  override val topic = "nodes/bridge/0/#"

  val bcSensorTopic = """nodes/bridge/0/([\w-]+)/([\w-]+)""".r

  override def messageReceived(receivedTopic: String, json: JsValue): Unit = {
    receivedTopic match {
      case bcSensorTopic(sensor, _) =>
        Json.fromJson(json)(BcMessage.reads) match {
          case JsSuccess(msg: BcMessage, _) =>
            val measure = new BcMeasure(sensor, msg.phenomenon, clock.instant(), msg.value, msg.unit)
            bcDao.save(measure)
          case JsError(_) => Logger.error(s"Parsing $json failed");
        }
    }
  }
}
