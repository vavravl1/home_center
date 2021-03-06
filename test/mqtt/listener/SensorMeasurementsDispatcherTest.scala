package mqtt.listener

import java.time.{Clock, Instant}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import model.location.impl.{LocationRepositorySql, LocationSql}
import model.sensor.impl.{DoubleValuesMeasuredPhenomenonInflux, MeasuredPhenomenonInflux, SensorRepositorySql, SensorSql}
import model.sensor.{DoubleValuesMeasurementAggregationStrategy, Measurement}
import mqtt.clown.MqttBigClownParser
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}


class SensorMeasurementsDispatcherTest extends WordSpec with Matchers with MockFactory {
  "BigClownStoringListener" when {

    implicit val system = ActorSystem()
    val instant = Instant.ofEpochSecond(22)
    val clock = mock[Clock]
    val locationRepository = mock[LocationRepositorySql]
    val sensorRepository = mock[SensorRepositorySqlWithCtor]
    val mqttBigClownParser = new MqttBigClownParser(
      sensorRepository, locationRepository, clock
    )

    "has no listeners" should {
      val dispatcher:TestActorRef[SensorMeasurementsDispatcher] = TestActorRef[SensorMeasurementsDispatcher](Props(new SensorMeasurementsDispatcher(
        system, mqttBigClownParser, Seq()
      )))
      val sensor = mock[SensorSqlWithCtor]

//      "not throw any exception with empty listeners" in {
//        dispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived("node/garage/pve-inverter/-/power", "42")
//      }

      "receive messages from thermometer" in {
        val phenomenon = mock[TemperatureMeasuredPhenomenon]
        (clock.instant: () => Instant).expects().returning(instant).anyNumberOfTimes()
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "thermometer").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("temperature", "\u2103", DoubleValuesMeasurementAggregationStrategy).returning(phenomenon)
        (phenomenon.addMeasurement _).expects(Measurement(19.19, Instant.ofEpochSecond(22)))

        dispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived(
          "node/836d19833c33/thermometer/0:0/temperature",
          "19.19"
        )
      }

      "receive messages from co2-meter" in {
        val phenomenon = mock[ConcentrationMeasuredPhenomenon]
        (clock.instant _).expects().returning(instant).anyNumberOfTimes()
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "co2-meter").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("concentration", "ppm", DoubleValuesMeasurementAggregationStrategy).returning(phenomenon)
        (phenomenon.addMeasurement _).expects(Measurement(1001, Instant.ofEpochSecond(22)))

        dispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived(
          "node/836d19833c33/co2-meter/-/concentration",
          "1001"
        )
      }

      "receive messages from hygrometer" in {
        val phenomenon = mock[HumidityMeasuredPhenomenon]
        (clock.instant _).expects().returning(instant).anyNumberOfTimes()
        val location = LocationSql("836d19833c33", "label")

        (locationRepository.findOrCreateLocation _).expects("836d19833c33").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "hygrometer").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("relative-humidity", "%", DoubleValuesMeasurementAggregationStrategy).returning(phenomenon)
        (phenomenon.addMeasurement _).expects(Measurement(56.6, Instant.ofEpochSecond(22)))

        dispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived(
          "node/836d19833c33/hygrometer/0:4/relative-humidity",
          "56.6"
        )
      }

      "receive messages from pve-inverter" in {
        val phenomenon = mock[PveMeasuredPhenomenon]
        val location = LocationSql("garage", "garage")

        (locationRepository.findOrCreateLocation _).expects("garage").returning(location)
        (sensorRepository.findOrCreateSensor _).expects(location, "pve-inverter").returning(sensor)
        (sensor.findOrCreatePhenomenon _).expects("power", "W", DoubleValuesMeasurementAggregationStrategy).returning(phenomenon)
        (phenomenon.addMeasurement _).expects(Measurement(850, Instant.ofEpochSecond(1200)))

        dispatcher ! SensorMeasurementsDispatcherMessages.MessageReceived(
          "node/garage/pve-inverter/-/power",
          "850,1200"
        )
      }
    }
  }

  class SensorRepositorySqlWithCtor extends SensorRepositorySql(null, null, null)
  class SensorSqlWithCtor extends SensorSql(null, null, null, null, null)
  class TemperatureMeasuredPhenomenon extends DoubleValuesMeasuredPhenomenonInflux("temperature", "\u2103", DoubleValuesMeasurementAggregationStrategy, null, SensorSql(null, null, null,null,null) ,null, null)
  class ConcentrationMeasuredPhenomenon extends DoubleValuesMeasuredPhenomenonInflux("concentration", "ppm", DoubleValuesMeasurementAggregationStrategy, null, SensorSql(null, null, null,null,null), null, null)
  class HumidityMeasuredPhenomenon extends DoubleValuesMeasuredPhenomenonInflux("relative-humidity", "%", DoubleValuesMeasurementAggregationStrategy, null, SensorSql(null, null, null,null,null), null, null)
  class PveMeasuredPhenomenon extends DoubleValuesMeasuredPhenomenonInflux("power", "W", DoubleValuesMeasurementAggregationStrategy, null, SensorSql(null, null, null,null,null), null, null)
}
