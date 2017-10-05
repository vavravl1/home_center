package model

import java.time.temporal.ChronoUnit._
import java.time.{Clock, Instant}

import dao.{ByDay, ByHour, DbTest}
import model.sensor.impl.MeasuredPhenomenonSql
import model.sensor.{IdentityMeasurementAggregationStrategy, Measurement, SingleValueAggregationStrategy}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import scalikejdbc._

/**
  *
  */
class SensorSqlTest extends WordSpec with Matchers with DbTest with MockFactory {
  override lazy val clock = mock[Clock]

  "SensorSql" when {
    val i = Instant.ofEpochSecond(0)
    val location = locationRepository.findOrCreateLocation("remote/0")
    location.updateLabel("upstairs corridor")

    sensorRepository.findAll()
      .foreach(s => sensorRepository.delete(s))
    val sensor = sensorRepository.findOrCreateSensor(location, "thermometer")
    val temperaturePhenomenon = sensor.findOrCreatePhenomenon("temperature", "C", IdentityMeasurementAggregationStrategy).asInstanceOf[MeasuredPhenomenonSql]
    val powerStatsPhenomenon = sensor.findOrCreatePhenomenon("zL1-cons", "kWh", SingleValueAggregationStrategy).asInstanceOf[MeasuredPhenomenonSql]

    "given several temperatures" should {
      sensor.addMeasurement(Measurement(10, i), temperaturePhenomenon)
      sensor.addMeasurement(Measurement(20, i.plus(30, MINUTES)), temperaturePhenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(70, MINUTES)), temperaturePhenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(80, MINUTES)), temperaturePhenomenon)
      sensor.addMeasurement(Measurement(60, i.plus(90, MINUTES)), temperaturePhenomenon)

      "correctly sample the temperatures" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes

        sensor.location.address shouldBe "remote/0"
        sensor.location.label shouldBe "upstairs corridor"
        sensor.name shouldBe "thermometer"
        sensor.measuredPhenomenons.head.name shouldBe "temperature"
        sensor.measuredPhenomenons.head.unit shouldBe "C"

        sensor.measuredPhenomenons.head.measurements(ByHour)(0).min shouldBe 10
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).max shouldBe 20
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).average shouldBe 15
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).measureTimestamp shouldBe i.plus(30, MINUTES)

        sensor.measuredPhenomenons.head.measurements(ByHour)(1).min shouldBe 30
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).max shouldBe 60
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).average shouldBe 40
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).measureTimestamp shouldBe i.plus(90, MINUTES)
      }

      "correctly group the temperatures" in {
        (clock.instant _).expects().returning(i.plus(3, HOURS)).anyNumberOfTimes
        sensor.aggregateOldMeasurements()

        sensor.measuredPhenomenons.head.measurements(ByHour)(0).min shouldBe 15
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).max shouldBe 15
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).average shouldBe 15
        sensor.measuredPhenomenons.head.measurements(ByHour)(0).measureTimestamp shouldBe i.plus(30, MINUTES)

        sensor.measuredPhenomenons.head.measurements(ByHour)(1).min shouldBe 40
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).max shouldBe 40
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).average shouldBe 40
        sensor.measuredPhenomenons.head.measurements(ByHour)(1).measureTimestamp shouldBe i.plus(90, MINUTES)

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM measurement WHERE measuredPhenomenonId=${temperaturePhenomenon.sensorId}""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }
    
    "given several singleValueAggregated wattrouter-stats" should {
      sensor.addMeasurement(Measurement(15, i), powerStatsPhenomenon)
      sensor.addMeasurement(Measurement(25, i.plus(30, MINUTES)), powerStatsPhenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(1, DAYS).plus(5, HOURS)), powerStatsPhenomenon)
      sensor.addMeasurement(Measurement(60, i.plus(1, DAYS).plus(8, HOURS)), powerStatsPhenomenon)

      "correctly group the singleValueAggregated wattrouter-stats" in {
        (clock.instant _).expects().returning(i.plus(4, DAYS)).anyNumberOfTimes
        sensor.aggregateOldMeasurements()

        sensor.measuredPhenomenons(1).measurements(ByDay)(0).min shouldBe 25
        sensor.measuredPhenomenons(1).measurements(ByDay)(0).max shouldBe 25
        sensor.measuredPhenomenons(1).measurements(ByDay)(0).average shouldBe 25
        sensor.measuredPhenomenons(1).measurements(ByDay)(0).measureTimestamp shouldBe i.plus(30, MINUTES)

        sensor.measuredPhenomenons(1).measurements(ByDay)(1).min shouldBe 60
        sensor.measuredPhenomenons(1).measurements(ByDay)(1).max shouldBe 60
        sensor.measuredPhenomenons(1).measurements(ByDay)(1).average shouldBe 60
        sensor.measuredPhenomenons(1).measurements(ByDay)(1).measureTimestamp shouldBe i.plus(1, DAYS).plus(8, HOURS)

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM measurement WHERE measuredPhenomenonId=${powerStatsPhenomenon.sensorId}""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }

    "computes areAllMeasuredPhenomenonsSingleValue" should {
      val i = Instant.ofEpochSecond(0)
      val location = locationRepository.findOrCreateLocation("remote/2")

      sensorRepository.find(location, "wattmeter-stats").map(s => sensorRepository.delete(s))
      sensorRepository.find(location, "hybrid-sensor").map(s => sensorRepository.delete(s))

      "return true for all measurements of only singleValue only" in {
        val sensor = sensorRepository.findOrCreateSensor(location, "wattmeter-stats")
        val phenomenon = sensor.findOrCreatePhenomenon("l1Cons", "kWh", SingleValueAggregationStrategy)
        sensor.addMeasurement(Measurement(1.2, i), phenomenon)
        sensor.addMeasurement(Measurement(1.4, i.plus(30, MINUTES)), phenomenon)

        sensor.areAllMeasuredPhenomenonsSingleValue shouldBe true
      }

      "return false for several measurement types" in {
        val sensor = sensorRepository.findOrCreateSensor(location, "hybrid-sensor")
        val phenomenonA = sensor.findOrCreatePhenomenon("l1Cons", "kWh", SingleValueAggregationStrategy)
        sensor.addMeasurement(Measurement(1.2, i), phenomenonA)
        val phenomenonB = sensor.findOrCreatePhenomenon("temperature", "C", IdentityMeasurementAggregationStrategy)
        sensor.addMeasurement(Measurement(10, i), phenomenonB)

        sensor.areAllMeasuredPhenomenonsSingleValue shouldBe false
      }
    }
  }
}
