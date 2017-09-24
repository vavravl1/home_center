package model.sensor.impl

import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Clock, Instant}

import dao.{ByMinuteBig, DbTest}
import model.sensor.{IdentityMeasurementAggregationStrategy, Measurement}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

/**
  *
  */
class MeasuredPhenomenonSqlTest extends WordSpec with Matchers with DbTest with MockFactory {
  override lazy val clock = mock[Clock]

  "MeasuredPhenomenonSql" when {
    val location = locationRepository.findOrCreateLocation("remote/1")
    sensorRepository.delete(sensorRepository.findOrCreateSensor(location, "thermometer")) // Clean up
    val sensor = sensorRepository.findOrCreateSensor(location, "thermometer")
    val phenomenon = sensor.findOrCreatePhenomenon(
      name = "temperature",
      unit = "Celsius",
      aggregationStrategy = IdentityMeasurementAggregationStrategy
    )

    "given 3 measurements" should {
      val i = Instant.ofEpochSecond(0)

      sensor.addMeasurement(Measurement(10, i), phenomenon)
      sensor.addMeasurement(Measurement(20, i.plus(20, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(30, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(40, i.plus(40, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(50, i.plus(50, MINUTES)), phenomenon)

      "correctly returns measurements by time aggregation" in {
        (clock.instant _).expects().returning(i).once()
        val measurements = phenomenon.measurements(ByMinuteBig)
        measurements should have size 5
        measurements.map(_.average) shouldBe Seq(10, 20, 30, 40, 50)
      }

      "correctly returns last 3" in {
        val last3Measurements = phenomenon.lastNMeasurementsDescendant(3)
        last3Measurements should have size 3
        last3Measurements.map(_.average) shouldBe Seq(50, 40, 30)
      }
    }
  }
}
