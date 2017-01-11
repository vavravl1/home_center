package mqtt.watering

import java.time.Clock

import dao.WateringDao
import entities.watering.WateringMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class WateringListenerTest extends WordSpec with Matchers with MockFactory {
  "WateringListener" when {
    val clock = mock[Clock]
    val wateringDao = mock[WateringDaoWithCtor]
    val wateringListener = new WateringListener(wateringDao)

    "is asked if it is applied" should {
      "returns true for correct topic" in {
        wateringListener.applies("home/watering/ibisek/telemetry") shouldBe true
      }
      "returns false for correct incorrect topic" in {
        wateringListener.applies("home/watering/kopretina/telemetry") shouldBe false
      }
    }

    "receives the message" should {
      "store it in dao if the message is correct" in {
        val correctJson = Json.parse(""" {"ts":8119,"tm":{"hu":{"a":86,"bl":512,"md":1000,"bs":10,"pd":30},"wa":{"ip":false,"wp":20000,"wt":5000},"wlh":true}}""")
        (wateringDao.save _).expects(Json.fromJson(correctJson)(WateringMessage.wmReads).get)
        //        (wateringDao.save _).expects(Json.fromJson(correctJson)(WateringMessage.wmReads))
        wateringListener.messageReceived("home/watering/ibisek/telemetry", correctJson)
      }
    }

    class WateringDaoWithCtor extends WateringDao(clock)
  }
}
