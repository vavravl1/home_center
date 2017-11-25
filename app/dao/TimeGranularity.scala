package dao

import java.sql.Timestamp
import java.time.temporal.ChronoUnit.{DAYS, MINUTES, SECONDS}
import java.time.{Clock, Instant}

import loader._
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax

/**
  * Represents granularity by which the data should be returned
  */
sealed abstract class TimeGranularity {
  def toExtractAndTime()(implicit clock: Clock):(SQLSyntax, SQLSyntax, Timestamp) = {
    this match {
      case BySecond => (sqls"SECOND", sqls"MINUTE", new Timestamp(clock.instant().minus(30, SECONDS).toEpochMilli))
      case BySecondBig => (sqls"SECOND", sqls"MINUTE", new Timestamp(clock.instant().minus(1, MINUTES).toEpochMilli))
      case ByMinute => (sqls"MINUTE", sqls"HOUR", new Timestamp(clock.instant().minus(30, MINUTES).toEpochMilli))
      case ByMinuteBig => (sqls"MINUTE", sqls"HOUR", new Timestamp(clock.instant().minus(120, MINUTES).toEpochMilli))
      case ByHour => (sqls"HOUR", sqls"DAY", new Timestamp(clock.instant().minus(1, DAYS).toEpochMilli))
      case ByHourBig => (sqls"HOUR", sqls"DAY", new Timestamp(clock.instant().minus(2, DAYS).toEpochMilli))
      case ByDay => (sqls"DAY", sqls"MONTH", new Timestamp(clock.instant().minus(14, DAYS).toEpochMilli))
      case ByDayBig => (sqls"DAY", sqls"MONTH", new Timestamp(clock.instant().minus(30, DAYS).toEpochMilli))
      case ByMonth => (sqls"MONTH", sqls"YEAR", new Timestamp(clock.instant().minus(365, DAYS).toEpochMilli))
      case ByMonthBig => (sqls"MONTH", sqls"YEAR", new Timestamp(clock.instant().minus(2*365, DAYS).toEpochMilli))
    }
  }

  def toExtractAndTimeForInflux()(implicit clock: Clock):(RetentionPolicy, String, Instant) = {
    this match {
      case BySecond => (OneHourRetentionPolicy, "1s", clock.instant().minus(30, SECONDS))
      case BySecondBig => (OneHourRetentionPolicy, "1s", clock.instant().minus(3, MINUTES))
      case ByMinute => (FourDaysRetentionPolicy, "1m", clock.instant().minus(30, MINUTES))
      case ByMinuteBig => (FourDaysRetentionPolicy, "1m", clock.instant().minus(120, MINUTES))
      case ByHour => (FourDaysRetentionPolicy, "1h", clock.instant().minus(1, DAYS))
      case ByHourBig => (FourDaysRetentionPolicy, "1h", clock.instant().minus(2, DAYS))
      case ByDay => (ForeverRetentionPolicy, "1d", clock.instant().minus(14, DAYS))
      case ByDayBig => (ForeverRetentionPolicy, "1d", clock.instant().minus(30, DAYS))
      case ByMonth => (ForeverRetentionPolicy, "30d", clock.instant().minus(365, DAYS))
      case ByMonthBig => (ForeverRetentionPolicy, "30d", clock.instant().minus(2*365, DAYS))
    }
  }
}

object TimeGranularity {
  def parse(str: String, big: Boolean): TimeGranularity = str match {
    case "BySecond" if !big => BySecond
    case "BySecond" if big => BySecondBig
    case "ByMinute" if !big => ByMinute
    case "ByMinute" if big => ByMinuteBig
    case "ByHour" if !big => ByHour
    case "ByHour" if big => ByHourBig
    case "ByDay" if !big => ByDay
    case "ByDay" if big => ByDayBig
    case "ByMonth" if !big => ByMonth
    case "ByMonth" if big => ByMonthBig
    case _ => ByHour
  }
}

object BySecond extends TimeGranularity
object BySecondBig extends TimeGranularity
object ByMinute extends TimeGranularity
object ByMinuteBig extends TimeGranularity
object ByHour extends TimeGranularity
object ByHourBig extends TimeGranularity
object ByDay extends TimeGranularity
object ByDayBig extends TimeGranularity
object ByMonth extends TimeGranularity
object ByMonthBig extends TimeGranularity