package mqtt

import akka.actor.{ActorPath, ActorSystem}
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class MqttDispatchingMqttListenerTest extends WordSpec with Matchers with MockFactory {
  "MqttDispatchingListener" when {
    val msg = """{"key":"value"}""".getBytes()
    val topic = "test_topic"

    "has no listeners" should {
      val actorSystem = mock[ActorSystem]
      val mqttListener = new MqttDispatchingListener(actorSystem)

      "not throw any exception" in {
        mqttListener.messageArrived("topic", new MqttMessage(msg))
      }
    }

//    "given two listeners and simple message and topic" should {
//      val actorSystem = mock[ActorSystem]
//      val mqttListener = new MqttDispatchingListener(actorSystem)
//      mqttListener.addListener(ActorPath.)
//
//      "calls correct callback" in {
//        (testListenerA.applies _).expects(topic).returning(true)
//        (testListenerA.messageReceived _).expects(topic, Json.parse(msg))
//        (testListenerB.applies _).expects(topic).returning(false)
//
//        mqttListener.messageArrived(topic, new MqttMessage(msg))
//      }
//    }
  }
}
