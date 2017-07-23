package mqtt.watering

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import model.location.impl.{LocationRepositorySql, LocationSql}
import model.sensor._
import model.sensor.impl.SensorRepositorySql
import mqtt.MqttListenerMessage.ConsumeMessage
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

/**
  *
  */
class WateringListenerTest extends WordSpec with Matchers with MockFactory {
  "WateringListener" when {

    implicit val system = ActorSystem()
    val clock = mock[Clock]
    val instant = Instant.ofEpochSecond(22)
    (clock.instant _).expects().returning(instant).atLeastOnce()

    "receives the message" should {
      "store it in dao if the message is correct" in {
        val locationRepository = mock[LocationRepositorySql]
        val sensorRepository = mock[SensorRepositorySqlWithCtor]
        val listener = TestActorRef[WateringListener](Props(wire[WateringListener]))
        val sensor = mock[Sensor]
        val humidityPhenomenon = mock[MeasuredPhenomenon]
        val wateringPhenomenon = mock[MeasuredPhenomenon]
        val location = LocationSql("watering-ibiscus", "label")

        (locationRepository.findOrCreateLocation _).expects("watering-ibiscus").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "watering").returning(sensor)

        (sensor.findOrCreatePhenomenon _).expects( "humidity", "", IdentityMeasurementAggregationStrategy).returning(humidityPhenomenon)
        (sensor.addMeasurement _).expects(Measurement(86, Instant.ofEpochSecond(22)), humidityPhenomenon)

        (sensor.findOrCreatePhenomenon _).expects( "watering", "", BooleanMeasurementAggregationStrategy).returning(wateringPhenomenon)
        (sensor.addMeasurement _).expects(Measurement(10, Instant.ofEpochSecond(22)), wateringPhenomenon)

        val correctJson = """ {"ts":8119,"tm":{"hu":{"a":86,"bl":512,"md":1000,"bs":10,"pd":30},"wa":{"ip":true,"wp":20000,"wt":5000},"wlh":true}}"""
        listener ! ConsumeMessage("home/watering/ibisek/telemetry", correctJson)
      }
    }
  }

  class SensorRepositorySqlWithCtor extends SensorRepositorySql(null, null)
}
