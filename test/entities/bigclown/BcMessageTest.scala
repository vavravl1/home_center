package entities.bigclown

import mqtt.clown.BcMessage
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
          Json.fromJson(json)(BcMessage.reads).get.msgs.head.value shouldBe 19.19
          Json.fromJson(json)(BcMessage.reads).get.msgs.head.unit shouldBe "\u2103"
      }
    }

    "created from complicated json text" should {
      val  json = Json.parse("""{"altitude":[176.2,"m"],"pressure":[99.22,"kPa"]}""")
      "should parse correctly" in {
        Json.fromJson(json)(BcMessage.reads).get.msgs.head.phenomenon shouldBe "altitude"
        Json.fromJson(json)(BcMessage.reads).get.msgs.head.value shouldBe 176.2
        Json.fromJson(json)(BcMessage.reads).get.msgs.head.unit shouldBe "m"

        Json.fromJson(json)(BcMessage.reads).get.msgs.tail.head.phenomenon shouldBe "pressure"
        Json.fromJson(json)(BcMessage.reads).get.msgs.tail.head.value shouldBe 99.22
        Json.fromJson(json)(BcMessage.reads).get.msgs.tail.head.unit shouldBe "kPa"

      }
    }
  }

  "CarbonDioxide" when {
    "created from json text" should {
      val  json = Json.parse("{\"concentration\": [786, \"ppm\"]}")
      "should parse correctly" in {
          Json.fromJson(json)(BcMessage.reads).get.msgs.head.value shouldBe 786
      }
    }
  }
}
