package model

import java.time.temporal.ChronoUnit._
import java.time.{Clock, Instant}

import dao.{ByHour, DbTest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import scalikejdbc._

/**
  *
  */
class SensorSqlTest extends WordSpec with Matchers with DbTest with MockFactory {
  override lazy val clock = mock[Clock]

  "SensorSql" when {
    "filled by measures over last 3 hours" should {
      val i = Instant.ofEpochSecond(0)
      val location = locationRepository.findOrCreateLocation("remote/0")
      location.updateLabel("upstairs corridor")

      sensorRepository.findAll()
        .foreach(s => sensorRepository.delete(s))
      val sensor = sensorRepository.findOrCreateSensor(location, "thermometer")
      val phenomenon = sensor.findOrCreatePhenomenon("temperature", "C", NoneMeasurementAggregationStrategy)
      sensor.addMeasurement(Measurement(10, i), phenomenon)
      sensor.addMeasurement(Measurement(20, i.plus(30, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(70, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(30, i.plus(80, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(60, i.plus(90, MINUTES)), phenomenon)

      "correctly samples the temperatures" in {
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

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM measurement""".map(rs => rs.int(1)).single.apply() shouldBe Some(5)
        })
      }

      "correctly groups the temperatures" in {
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
          sql"""SELECT COUNT(*) FROM measurement""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }
  }
}
