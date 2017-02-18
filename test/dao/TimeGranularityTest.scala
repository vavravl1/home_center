package dao

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Clock, Instant}

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers, WordSpec}
import scalikejdbc._

/**
  *
  */
class TimeGranularityTest extends WordSpec with Matchers with MockFactory {
  "TimeGranularity" when {
    "given as string" should {
      val byMinute = "ByMinute"
      val byHour = "ByHour"
      val byDay = "ByDay"
      val nonsense = "nonsense"
      "parse correctly" in {
        TimeGranularity.parse(byMinute) shouldBe ByMinute
        TimeGranularity.parse(byHour) shouldBe ByHour
        TimeGranularity.parse(byDay) shouldBe ByDay
      }
      "fallback to byHour if can't be parsed" in {
        TimeGranularity.parse(nonsense) shouldBe ByHour
      }
    }
    "given as class" should {
      "produce correct extracts and times for by minutes" in {
        implicit val clock = mock[Clock]
        (clock.instant _).expects().returning(
          Instant.ofEpochMilli(0).plus(30, ChronoUnit.MINUTES)
        ).anyNumberOfTimes
        ByMinute.toExtractAndTime() shouldBe (sqls"MINUTE", new Timestamp(0))
      }
      "produce correct extracts and times for by hours" in {
        implicit val clock = mock[Clock]
        (clock.instant _).expects().returning(
          Instant.ofEpochMilli(0).plus(1, ChronoUnit.DAYS)
        ).anyNumberOfTimes
        ByHour.toExtractAndTime() shouldBe (sqls"HOUR", new Timestamp(0))
      }
      "produce correct extracts and times for by days" in {
        implicit val clock = mock[Clock]
        (clock.instant _).expects().returning(
          Instant.ofEpochMilli(0).plus(14, ChronoUnit.DAYS)
        ).anyNumberOfTimes
        ByDay.toExtractAndTime() shouldBe (sqls"DAY", new Timestamp(0))
      }
    }
  }
}
