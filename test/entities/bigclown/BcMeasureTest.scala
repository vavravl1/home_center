package entities.bigclown

import java.time.Instant

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class BcMeasureTest extends WordSpec with Matchers   {
  "BcMeasure" when {
    "created" should {
      val measure = BcMeasure("remote/1", "thermometer", "temperature", Instant.ofEpochSecond(10), 19, "C")
      "should be serializable to json" in {
        Json.toJson(measure)(BcMeasure.format).toString() shouldBe """{"location":"remote/1","sensor":"thermometer","phenomenon":"temperature","measureTimestamp":"1970-01-01T00:00:10Z","value":19,"unit":"C"}"""
      }
    }
  }
}
