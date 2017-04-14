package dao

import java.sql.Timestamp
import java.time.Clock
import java.time.temporal.ChronoUnit.{DAYS, MINUTES, HOURS, MONTHS}

import scalikejdbc._

/**
  * Represents granularity by which the data should be returned
  */
sealed abstract class TimeGranularity {
  def toExtractAndTime()(implicit clock: Clock) = {
    this match {
      case ByMinute => (sqls"MINUTE", new Timestamp(clock.instant().minus(30, MINUTES).toEpochMilli))
      case ByMinuteBig => (sqls"MINUTE", new Timestamp(clock.instant().minus(120, MINUTES).toEpochMilli))
      case ByHour => (sqls"HOUR", new Timestamp(clock.instant().minus(1, DAYS).toEpochMilli))
      case ByHourBig => (sqls"HOUR", new Timestamp(clock.instant().minus(2, DAYS).toEpochMilli))
      case ByDay => (sqls"DAY", new Timestamp(clock.instant().minus(14, DAYS).toEpochMilli))
      case ByDayBig => (sqls"DAY", new Timestamp(clock.instant().minus(30, DAYS).toEpochMilli))
    }
  }
}

object TimeGranularity {
  def parse(str: String, big: Boolean): TimeGranularity = str match {
    case "ByMinute" if !big => ByMinute
    case "ByMinute" if big => ByMinuteBig
    case "ByHour" if !big => ByHour
    case "ByHour" if big => ByHourBig
    case "ByDay" if !big => ByDay
    case "ByDay" if big => ByDayBig
    case _ => ByHour
  }
}

object ByMinute extends TimeGranularity
object ByMinuteBig extends TimeGranularity
object ByHour extends TimeGranularity
object ByHourBig extends TimeGranularity
object ByDay extends TimeGranularity
object ByDayBig extends TimeGranularity