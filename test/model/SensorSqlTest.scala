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

  "BcMeasureDao" when {
    "filled by measures over last 3 hours" should {
      val i = Instant.ofEpochSecond(0)
      val location = locationRepository.findOrCreateLocation("remote/0")
      location.updateLabel("upstairs corridor")

      sensorRepository.findAll()
        .foreach(s => sensorRepository.delete(s.location.address, s.measuredPhenomenon))
      val sensor = sensorRepository.findOrCreateSensor("remote/0", "thermometer", "temperature", "C")
      sensor.addMeasurement(Measurement(10, i, false))
      sensor.addMeasurement(Measurement(20, i.plus(30, MINUTES), false))
      sensor.addMeasurement(Measurement(30, i.plus(70, MINUTES), false))
      sensor.addMeasurement(Measurement(30, i.plus(80, MINUTES), false))
      sensor.addMeasurement(Measurement(60, i.plus(90, MINUTES), false))

      "correctly samples the temperatures" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes

        sensor.location.address shouldBe "remote/0"
        sensor.location.label shouldBe "upstairs corridor"
        sensor.name shouldBe "thermometer"
        sensor.measuredPhenomenon shouldBe "temperature"
        sensor.unit shouldBe "C"

        sensor.getAggregatedValues(ByHour)(0).min shouldBe 10
        sensor.getAggregatedValues(ByHour)(0).max shouldBe 20
        sensor.getAggregatedValues(ByHour)(0).average shouldBe 15
        sensor.getAggregatedValues(ByHour)(0).measureTimestamp shouldBe i.plus(30, MINUTES)

        sensor.getAggregatedValues(ByHour)(1).min shouldBe 30
        sensor.getAggregatedValues(ByHour)(1).max shouldBe 60
        sensor.getAggregatedValues(ByHour)(1).average shouldBe 40
        sensor.getAggregatedValues(ByHour)(1).measureTimestamp shouldBe i.plus(90, MINUTES)

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM measurement""".map(rs => rs.int(1)).single.apply() shouldBe Some(5)
        })
      }

      "correctly groups the temperatures" in {
        (clock.instant _).expects().returning(i.plus(3, HOURS)).anyNumberOfTimes
        sensor.aggregateOldMeasurements()

        sensor.getAggregatedValues(ByHour)(0).min shouldBe 15
        sensor.getAggregatedValues(ByHour)(0).max shouldBe 15
        sensor.getAggregatedValues(ByHour)(0).average shouldBe 15
        sensor.getAggregatedValues(ByHour)(0).measureTimestamp shouldBe i.plus(30, MINUTES)

        sensor.getAggregatedValues(ByHour)(1).min shouldBe 40
        sensor.getAggregatedValues(ByHour)(1).max shouldBe 40
        sensor.getAggregatedValues(ByHour)(1).average shouldBe 40
        sensor.getAggregatedValues(ByHour)(1).measureTimestamp shouldBe i.plus(90, MINUTES)

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM measurement""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }
  }
}
