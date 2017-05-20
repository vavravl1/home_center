package mqtt.clown

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import model.impl.{LocationRepositorySql, SensorRepositorySql}
import model.{MeasuredPhenomenon, Measurement, NoneMeasurementAggregationStrategy, Sensor}
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
        val locationRepository = mock[LocationRepositorySql]
        val sensorRepository = mock[SensorRepositorySqlWithCtor]
        val listener = TestActorRef[BridgeListener](Props(wire[BridgeListener]))
        val sensor = mock[Sensor]
        val phenomenon = mock[MeasuredPhenomenon]

        (locationRepository.findOrCreateLocation _).expects("bridge-0")
        (sensorRepository.findOrCreateSensor _).expects("bridge-0", "thermometer").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects( "temperature", "\u2103", NoneMeasurementAggregationStrategy).returning(phenomenon)
        (sensor.addMeasurement _).expects(Measurement(19.19, Instant.ofEpochSecond(22)),phenomenon)

        listener ! ConsumeMessage(
          "node/bridge/0/thermometer/i2c0-48",
          Json.parse("{\"temperature\": [19.19, \"\\u2103\"]}")
        )
      }
    }
  }

  class SensorRepositorySqlWithCtor extends SensorRepositorySql(null, null)
}
