package entities.watering

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json


class WateringMessageTest extends WordSpec with Matchers {

  "WateringMessage" when {
    "in basic form" should {
      val wateringMessage = WateringMessage(
        timestamp = Instant.ofEpochSecond(7),
        telemetry = WateringTelemetry(
          humidity = Humidity(
            actual = 200,
            measuringDelay = Duration.ofSeconds(15),
            baseLine = 300,
            bufferSize = 15,
            powerDelay = Duration.of(100, ChronoUnit.SECONDS)
          ),
          watering = Watering(
            inProgress = false,
            wateringPause = Duration.ofSeconds(20),
            wateringPumpTime = Duration.ofSeconds(5)
          ),
          waterLevelHigh = true
        )
      )
      "produce correct json" in {
        (Json.toJson(wateringMessage) \ "timestamp").as[Int] shouldBe 7000

        (Json.toJson(wateringMessage) \ "telemetry" \ "humidity" \ "actual").as[Int] shouldBe 200
        (Json.toJson(wateringMessage) \ "telemetry" \ "humidity" \ "measuringDelay").as[Int] shouldBe 15000
        (Json.toJson(wateringMessage) \ "telemetry" \ "humidity" \ "baseLine").as[Int] shouldBe 300
        (Json.toJson(wateringMessage) \ "telemetry" \ "humidity" \ "bufferSize").as[Int] shouldBe 15
        (Json.toJson(wateringMessage) \ "telemetry" \ "humidity" \ "powerDelay").as[Int] shouldBe 100000

        (Json.toJson(wateringMessage) \ "telemetry" \ "watering" \ "inProgress").as[Boolean] shouldBe false
        (Json.toJson(wateringMessage) \ "telemetry" \ "watering" \ "wateringPause").as[Int] shouldBe 20000
        (Json.toJson(wateringMessage) \ "telemetry" \ "watering" \ "wateringPumpTime").as[Int] shouldBe 5000

        (Json.toJson(wateringMessage) \ "telemetry" \ "waterLevelHigh").as[Boolean] shouldBe true
      }
    }
    "as json text" should {
      val json = Json.parse(""" {"ts":8119,"tm":{"hu":{"a":86,"bl":512,"md":1000,"bs":10,"pd":30},"wa":{"ip":false,"wp":20000,"wt":5000},"wlh":true}}""")
      "be serializable" in {
        val result:WateringMessage = Json.fromJson(json)(WateringMessage.wmReads).get

        result.timestamp shouldBe Instant.ofEpochSecond(8119)

        result.telemetry.humidity.actual shouldBe 86
        result.telemetry.humidity.baseLine shouldBe 512
        result.telemetry.humidity.measuringDelay shouldBe Duration.ofMillis(1000)
        result.telemetry.humidity.bufferSize shouldBe 10
        result.telemetry.humidity.powerDelay shouldBe Duration.ofMillis(30)

        result.telemetry.watering.inProgress shouldBe false
        result.telemetry.watering.wateringPause shouldBe Duration.ofMillis(20000)
        result.telemetry.watering.wateringPumpTime shouldBe Duration.ofMillis(5000)
      }
    }
  }
}