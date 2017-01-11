package mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class MqttDispatchingListenerTest extends WordSpec with Matchers with MockFactory {
  "MqttDispatchingListener" when {
    val msg = """{"key":"value"}""".getBytes()
    val topic = "test_topic"

    "has no listeners" should {
      val mqttListener = new MqttDispatchingListener(List())

      "not throw any exception" in {
        mqttListener.messageArrived("topic", new MqttMessage(msg))
      }
    }

    "given two listeners and simple message and topic" should {
      val testListenerA = mock[Listener]
      val testListenerB = mock[Listener]
      val mqttListener = new MqttDispatchingListener(List(testListenerA, testListenerB))

      "calls correct callback" in {
        (testListenerA.applies _).expects(topic).returning(true)
        (testListenerA.messageReceived _).expects(topic, Json.parse(msg))
        (testListenerB.applies _).expects(topic).returning(false)

        mqttListener.messageArrived(topic, new MqttMessage(msg))
      }
    }
  }
}
