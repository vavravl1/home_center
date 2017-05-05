package mqtt.watering

import java.time.Clock

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import dao.WateringDao
import entities.watering.WateringMessage
import mqtt.MqttListenerMessage.ConsumeMessage
import mqtt.clown.BridgeListener
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class WateringMqttListenerTest extends WordSpec with Matchers with MockFactory {
  "WateringListener" when {

    implicit val system = ActorSystem()
    val clock = mock[Clock]
    val wateringDao = mock[WateringDaoWithCtor]
    val wateringListener = TestActorRef[BridgeListener](Props(wire[WateringListener]))

    "receives the message" should {
      "store it in dao if the message is correct" in {
        val correctJson = Json.parse(""" {"ts":8119,"tm":{"hu":{"a":86,"bl":512,"md":1000,"bs":10,"pd":30},"wa":{"ip":false,"wp":20000,"wt":5000},"wlh":true}}""")
        (wateringDao.save _).expects(Json.fromJson(correctJson)(WateringMessage.wmReads).get)
        wateringListener ! ConsumeMessage("home/watering/ibisek/telemetry", correctJson)
      }
    }

    class WateringDaoWithCtor extends WateringDao(clock)
  }
}
