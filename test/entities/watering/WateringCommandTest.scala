package entities.watering

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import mqtt.watering._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class WateringCommandTest extends WordSpec with Matchers {
  "WateringCommand" when {
    "with single manual watering command" should {
      val wc = WateringCommand(Set(ManualWateringCommand()))
      "produces correct json" in {
        (Json.toJson(wc) \ "command" \ "manual-watering").as[Boolean] shouldBe true
      }
    }
    "with all commands" should {
      val wc = WateringCommand(Set(
        ManualWateringCommand(),
        TimeCommand(Instant.ofEpochSecond(33)),
        HumidityMeasuringDelay(Duration.of(10, ChronoUnit.SECONDS)),
        HumidityMeasurePowerDelay(Duration.of(5, ChronoUnit.SECONDS)),
        HumidityBaseline(120),
        HumidityMeasureBufferSize(13),
        WateringPause(Duration.of(2, ChronoUnit.HOURS))
      ))
      "produces correct json" in {
        (Json.toJson(wc) \ "command" \ "manual-watering").as[Boolean] shouldBe true
        (Json.toJson(wc) \ "command" \ "set-time").as[Int] shouldBe 33
        (Json.toJson(wc) \ "command" \ "set-humidity-measuring-delay").as[Int] shouldBe 10000
        (Json.toJson(wc) \ "command" \ "set-humidity-measure-power-delay").as[Int] shouldBe 5000
        (Json.toJson(wc) \ "command" \ "set-humidity-baseline").as[Int] shouldBe 120
        (Json.toJson(wc) \ "command" \ "set-humidity-measure-buffer-size").as[Int] shouldBe 13
        (Json.toJson(wc) \ "command" \ "set-humidity-baseline").as[Int] shouldBe 120
        (Json.toJson(wc) \ "command" \ "set-watering-pause").as[Int] shouldBe 2 * 60 * 60 * 1000
      }
    }

  }
}
