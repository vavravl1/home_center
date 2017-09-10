package mqtt.clown

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import model.location.impl.{LocationRepositorySql, LocationSql}
import model.sensor.impl.SensorRepositorySql
import model.sensor.{IdentityMeasurementAggregationStrategy, MeasuredPhenomenon, Measurement, Sensor}
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

    "in default state" should {
      val locationRepository = mock[LocationRepositorySql]
      val sensorRepository = mock[SensorRepositorySqlWithCtor]
      val listener = TestActorRef[BridgeListener](Props(wire[BridgeListener]))
      val sensor = mock[Sensor]
      val phenomenon = mock[MeasuredPhenomenon]

      "receive messages from thermometer" in {
        (clock.instant _).expects().returning(instant).anyNumberOfTimes()
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "thermometer").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects(  "temperature", "\u2103", IdentityMeasurementAggregationStrategy).returning(phenomenon)
        (sensor.addMeasurement _).expects(Measurement(19.19, Instant.ofEpochSecond(22)),phenomenon)

        listener ! ConsumeMessage(
          "node/836d19833c33/thermometer/0:0/temperature",
          "19.19"
        )
      }

      "receive messages from co2-meter" in {
        (clock.instant _).expects().returning(instant).anyNumberOfTimes()
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "co2-meter").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("concentration", "ppm", IdentityMeasurementAggregationStrategy).returning(phenomenon)
        (sensor.addMeasurement _).expects(Measurement(1001, Instant.ofEpochSecond(22)),phenomenon)

        listener ! ConsumeMessage(
          "node/836d19833c33/co2-meter/-/concentration",
          "1001"
        )
      }

      "receive messages from pve-inverter" in {
        val location = LocationSql("garage", "garage")

        (locationRepository.findOrCreateLocation _).expects("garage").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "pve-inverter").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("power", "W", IdentityMeasurementAggregationStrategy).returning(phenomenon)
        (sensor.addMeasurement _).expects(Measurement(850, Instant.ofEpochSecond(1200)),phenomenon)

        listener ! ConsumeMessage(
          "node/garage/pve-inverter/-/power",
          "850,1200"
        )
      }
    }
  }

  class SensorRepositorySqlWithCtor extends SensorRepositorySql(null, null)
}
