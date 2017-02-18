package dao

import java.sql.Timestamp
import java.time.temporal.ChronoUnit.HOURS
import java.time.{Clock, Instant}

import _root_.play.api.Logger
import entities.watering.{Humidity, Watering, WateringMessage, WateringTelemetry}
import scalikejdbc.{DB, _}

/**
  * Dao for storing watering messages
  */
class WateringDao(_clock: Clock) {
  implicit val clock = _clock

  var lastMessage: Option[WateringMessage] = None

  def cleanDb(): Unit = {
    DB.autoCommit(implicit session => {
      sql"""TRUNCATE TABLE watering""".update().apply()
      lastMessage = None
    })
  }

  def save(message: WateringMessage): Unit = {
    DB.autoCommit(implicit session => {
      sql"""
          INSERT INTO watering (timestamp, actual_humidity, watering_in_progress)
          VALUES (
              ${message.timestamp},
              ${message.telemetry.humidity.actual},
              ${message.telemetry.watering.inProgress}
          )
      """
        .update.apply()
      lastMessage = Some(message)
    })
  }

  def getLastMessage(): Option[WateringMessage] = {
    lastMessage
  }

  def getAveragedMessages(by: TimeGranularity = ByHour): Seq[WateringMessage] = {
    lastMessage match {
      case None => Seq.empty
      case Some(last) =>
        DB.readOnly(implicit session => {
          val (extractTime, lastMeasureTimestamp) = by.toExtractAndTime

          sql"""
           SELECT MAX(timestamp) AS ts, AVG(actual_humidity) AS actual, BOOL_OR(watering_in_progress) AS wip
           FROM watering
           WHERE timestamp > ${lastMeasureTimestamp}
           GROUP BY EXTRACT(${extractTime} FROM timestamp)
           ORDER BY MAX(timestamp)
        """.map(rs => WateringMessage(
            Instant.ofEpochMilli(rs.timestamp("ts").millis),
            WateringTelemetry(
              Humidity(
                rs.int("actual"),
                last.telemetry.humidity.baseLine,
                last.telemetry.humidity.measuringDelay,
                last.telemetry.humidity.bufferSize,
                last.telemetry.humidity.powerDelay
              ),
              Watering(
                rs.boolean("wip"),
                last.telemetry.watering.wateringPause,
                last.telemetry.watering.wateringPumpTime
              ),
              last.telemetry.waterLevelHigh
            )
          )).toList().apply()
        })
    }
  }

  def sensorAggregation(): Unit = {
    DB.localTx(implicit session => {
      val lastHour = clock.instant().truncatedTo(HOURS).minus(1, HOURS)
      val lastHourTs = new Timestamp(lastHour.toEpochMilli)
      val aggregated =
        sql"""
           SELECT MAX(timestamp) AS ts, AVG(actual_humidity) AS actual, BOOL_OR(watering_in_progress) AS wip
           FROM watering
           WHERE timestamp < ${lastHourTs}
           AND aggregated = FALSE
           GROUP BY EXTRACT(HOUR FROM timestamp)
           ORDER BY MAX(timestamp)
        """
          .map(rs => (
            Instant.ofEpochMilli(rs.timestamp("ts").millis),
            rs.int("actual"),
            rs.boolean("wip")
          )).toList().apply()
      Logger.info(s"Loaded ${aggregated.size} waterings to aggregate")
      val updated =
        sql"""
           DELETE FROM watering
           WHERE timestamp < ${lastHourTs}
           AND aggregated = FALSE
         """
          .update.apply()
      Logger.info(s"Removed $updated watering rows")
      if (aggregated.size > 0) {
        sql"""
          INSERT INTO watering (timestamp, actual_humidity, watering_in_progress, aggregated)
          VALUES (?, ?, ?, ?)
          """
          .batch(aggregated.map(message => Seq(
            message._1,
            message._2,
            message._3,
            true
          )): _*).apply()
      }
    })
  }
}
