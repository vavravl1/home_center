package mqtt.clown

import java.time.Clock

import akka.actor.Actor
import dao.BcMeasureDao
import entities.bigclown.{BcMeasure, BcMessage}
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

/**
  * Stores messages from big clown
  */
class BridgeListener(bcDao: BcMeasureDao, clock: Clock) extends Actor {
  val bcSensorTopic = """nodes/(bridge|remote|base)/(\d+)/([\w-]+)/([\w-]+)""".r

  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, json: JsValue) => receivedTopic match {
      case bcSensorTopic(location, position, sensor, i2cAddress) =>
        Json.fromJson(json)(BcMessage.reads) match {
          case JsSuccess(messages: BcMessage, _) =>
            messages.msgs.map(msg =>
              new BcMeasure(
                location = location + "/" + position,
                sensor = sensor,
                phenomenon = msg.phenomenon,
                measureTimestamp = clock.instant(),
                value = msg.value,
                unit = msg.unit
              ))
              .foreach(measure => bcDao.save(measure))
          case JsError(_) => Logger.error(s"Parsing $json failed");
        }
      case _ => {}
    }
  }
}
