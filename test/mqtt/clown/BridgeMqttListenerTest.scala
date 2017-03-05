package mqtt.clown

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import dao.BcMeasureDao
import entities.bigclown.BcMeasure
import mqtt.MqttListenerMessage.ConsumeMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

/**
  *
  */
class BridgeMqttListenerTest extends WordSpec with Matchers with MockFactory {
  "BridgeListener" when {

    implicit val system = ActorSystem()
    val clock = mock[Clock]
    val instant = Instant.ofEpochSecond(22)
    (clock.instant _).expects().returning(instant).atLeastOnce()

    "in default state" should {
      "receive messages from thermometer" in {
        val dao = mock[BcMeasureDaoCtor]
        val listener = TestActorRef[BridgeListener](Props(wire[BridgeListener]))
        (dao.save _).expects(BcMeasure("thermometer", "temperature", instant, 19.19, "\u2103"))

        listener ! ConsumeMessage(
          "nodes/bridge/0/thermometer/i2c0-48",
          Json.parse("{\"temperature\": [19.19, \"\\u2103\"]}")
        )
      }
    }
    class BcMeasureDaoCtor extends BcMeasureDao(clock)
  }
}
