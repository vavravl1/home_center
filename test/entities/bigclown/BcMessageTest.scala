package entities.bigclown

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class BcMessageTest extends WordSpec with Matchers  {
  "Temperature" when {
    "created from json text" should {
      val  json = Json.parse("{\"temperature\": [19.19, \"\\u2103\"]}")
      "should parse correctly" in {
          Json.fromJson(json)(BcMessage.reads).get.value shouldBe 19.19
          Json.fromJson(json)(BcMessage.reads).get.unit shouldBe "\u2103"
      }
    }
  }

  "CarbonDioxide" when {
    "created from json text" should {
      val  json = Json.parse("{\"concentration\": [786, \"ppm\"]}")
      "should parse correctly" in {
          Json.fromJson(json)(BcMessage.reads).get.value shouldBe 786
      }
    }
  }
}
