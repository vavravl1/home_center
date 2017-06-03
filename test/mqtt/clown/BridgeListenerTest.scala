package mqtt.clown

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import model._
import model.impl.{LocationRepositorySql, LocationSql, SensorRepositorySql}
import mqtt.MqttListenerMessage.ConsumeMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}


/**
  *
  */
class BridgeListenerTest extends WordSpec with Matchers with MockFactory {
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
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "thermometer").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("temperature", "\u2103", NoneMeasurementAggregationStrategy).returning(phenomenon)
        (sensor.addMeasurement _).expects(Measurement(19.19, Instant.ofEpochSecond(22)),phenomenon)

        listener ! ConsumeMessage(
          "node/836d19833c33/thermometer/0:0/temperature",
          "19.19"
        )
      }
    }
  }

  class SensorRepositorySqlWithCtor extends SensorRepositorySql(null, null)
}
