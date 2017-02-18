package dao

import java.sql.Timestamp
import java.time.Clock
import java.time.temporal.ChronoUnit.{DAYS, MINUTES}

import scalikejdbc._

/**
  * Represents granularity by which the data should be returned
  */
sealed abstract class TimeGranularity {
  def toExtractAndTime()(implicit clock: Clock) = {
    this match {
      case ByMinute => (sqls"MINUTE", new Timestamp(clock.instant().minus(30, MINUTES).toEpochMilli))
      case ByHour => (sqls"HOUR", new Timestamp(clock.instant().minus(1, DAYS).toEpochMilli))
      case ByDay => (sqls"DAY", new Timestamp(clock.instant().minus(14, DAYS).toEpochMilli))
    }
  }
}

object TimeGranularity {
  def parse(str: String): TimeGranularity = str match {
    case "ByMinute" => ByMinute
    case "ByHour" => ByHour
    case "ByDay" => ByDay
    case _ => ByHour
  }
}

object ByMinute extends TimeGranularity
object ByHour extends TimeGranularity
object ByDay extends TimeGranularity