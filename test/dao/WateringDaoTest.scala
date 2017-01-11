package dao

import java.time.{Clock, Duration, Instant}

import entities.watering._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

/**
  *
  */
class WateringDaoTest extends WordSpec with Matchers with DbTest with MockFactory {
  override lazy val clock = mock[Clock]

  "WateringDao" when {
    val i = Instant.ofEpochSecond(0)
    "is empty" should {
      "produce empty option" in {
        wateringDao.cleanDb()
        wateringDao.getLastMessage() shouldBe None
      }
      "store and load single message" in {
        wateringDao.cleanDb()
        wateringDao.save(
          WateringMessage(
            Instant.ofEpochSecond(10),
            WateringTelemetry(
              Humidity(6, 2, Duration.ofDays(3), 3, Duration.ofHours(2)),
              Watering(true, Duration.ofMinutes(10), Duration.ofSeconds(2)),
              false
            )
          )
        )
        wateringDao.getLastMessage().get.telemetry.humidity.actual shouldBe 6
      }
    }
    "has one element" should {
      wateringDao.cleanDb()
      wateringDao.save(
        WateringMessage(
          Instant.ofEpochSecond(10),
          WateringTelemetry(
            Humidity(6, 2, Duration.ofDays(3), 3, Duration.ofHours(2)),
            Watering(true, Duration.ofMinutes(10), Duration.ofSeconds(2)),
            true
          ))
      )
      "accept another one" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes
        wateringDao.save(
          WateringMessage(
            Instant.ofEpochSecond(30),
            WateringTelemetry(
              Humidity(8, 3, Duration.ofDays(3), 3, Duration.ofHours(2)),
              Watering(true, Duration.ofMinutes(10), Duration.ofSeconds(2)),
              false
            )
          )
        )
        wateringDao.getLastMessage().get.telemetry.humidity.actual shouldBe 8
        wateringDao.getAveragedMessages()(0).telemetry.humidity.baseLine shouldBe 3
        wateringDao.getAveragedMessages()(0).telemetry.watering.wateringPumpTime.toMillis shouldBe 2000
      }
    }
    "has two elements" should {
      wateringDao.cleanDb()
      wateringDao.save(
        WateringMessage(
          Instant.ofEpochSecond(13),
          WateringTelemetry(
            Humidity(6, 2, Duration.ofDays(3), 3, Duration.ofHours(2)),
            Watering(true, Duration.ofMinutes(10), Duration.ofSeconds(2)),
            false
          )
        )
      )
      wateringDao.save(
        WateringMessage(
          Instant.ofEpochSecond(45),
          WateringTelemetry(
            Humidity(8, 3, Duration.ofDays(3), 3, Duration.ofHours(2)),
            Watering(true, Duration.ofMinutes(10), Duration.ofSeconds(2)),
            false
          )
        )
      )
      "have correct averaged results" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes
        wateringDao.sensorAggregation()
        wateringDao.getAveragedMessages() should have size 1
        wateringDao.getAveragedMessages()(0).telemetry.humidity.actual shouldBe 7
      }
    }
  }
}
