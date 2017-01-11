package mqtt.clown

import org.scalatest.{Matchers, WordSpec}

/**
  *
  */
class BridgeListenerTest extends WordSpec with Matchers {
  "BridgeListener" when {
    val listener = new BridgeListener(null, null)

    "in default state" should {
      "receive messages from thermometer" in {
        listener.applies("nodes/bridge/0/thermometer/i2c0-48") shouldBe true
      }
      "receive messages from humidity meter" in {
        listener.applies("nodes/bridge/0/humidity-sensor/i2c0-40") shouldBe true
      }
      "receive messages from lux meter" in {
        listener.applies("nodes/bridge/0/lux-meter/i2c0-44") shouldBe true
      }
      "receive messages from co2 meter" in {
        listener.applies("nodes/bridge/0/co2-sensor/i2c0-38") shouldBe true
      }
    }
  }
}
