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
        TimeGranularity.parse(byMinute, false) shouldBe ByMinute
        TimeGranularity.parse(byMinute, true) shouldBe ByMinuteBig
        TimeGranularity.parse(byHour, false) shouldBe ByHour
        TimeGranularity.parse(byHour, true) shouldBe ByHourBig
        TimeGranularity.parse(byDay, false) shouldBe ByDay
        TimeGranularity.parse(byDay, true) shouldBe ByDayBig
      }
      "fallback to byHour if can't be parsed" in {
        TimeGranularity.parse(nonsense, false) shouldBe ByHour
        TimeGranularity.parse(nonsense, true) shouldBe ByHour
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
      "produce correct extracts and times for by hours when big" in {
        implicit val clock = mock[Clock]
        (clock.instant _).expects().returning(
          Instant.ofEpochMilli(0).plus(2, ChronoUnit.DAYS)
        ).anyNumberOfTimes
        ByHourBig.toExtractAndTime() shouldBe (sqls"HOUR", new Timestamp(0))
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
