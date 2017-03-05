package mqtt.watering

import akka.actor.Actor
import dao.WateringDao
import entities.watering.WateringMessage
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from watering to dao
  */
class WateringListener(dao: WateringDao) extends Actor {
  val topic = "home/watering/ibisek/telemetry".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, json: JsValue) => receivedTopic match {
      case topic() => Json.fromJson(json)(WateringMessage.wmReads) match {
          case JsSuccess(value, _) => dao.save(value)
          case JsError(_) => Logger.error(s"Parsing $json failed");
        }
      case _ => {}
    }
  }
}
