package model.impl

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import _root_.play.Logger
import _root_.play.api.libs.json.{JsValue, Json, _}
import dao.TimeGranularity
import model.{MeasuredPhenomenon, Measurement}
import scalikejdbc._

/**
  *
  */
case class MeasuredPhenomenonSql(
                             override val name:String,
                             override val unit:String,
                             val id:String,
                             val sensorId:String,
                             val _clock: Clock
                           ) extends MeasuredPhenomenon {
  implicit val clock = _clock

  override def measurements(timeGranularity: TimeGranularity): Seq[Measurement] =
    DB.readOnly(implicit session => {
      val (extractTime, secondExtractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTime
      sql"""
           SELECT
            MAX(measureTimestamp) AS ts,
            ROUND(AVG(value), 2) AS avg,
            ROUND(MIN(value), 2) AS min,
            ROUND(MAX(value), 2) AS max
           FROM measurement
           WHERE measuredPhenomenonId = ${id}
            AND measureTimestamp > ${lastMeasureTimestamp}
           GROUP BY
            EXTRACT(${extractTime} FROM measureTimestamp),
            EXTRACT(${secondExtractTime} FROM measureTimestamp)
           ORDER BY MAX(measureTimestamp)
        """
        .map(measurementFromRs).toList().apply()
    })

  override def aggregateOldMeasurements(): Unit = DB.localTx(implicit session => {
    val oneHourAgo = clock.instant().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS)
    val lastHourTs = new Timestamp(oneHourAgo.toEpochMilli)
    val toAggregate =
      sql"""
             SELECT
              MAX(measureTimestamp) AS ts,
              ROUND(AVG(value), 2) AS avg,
              ROUND(MIN(value), 2) AS min,
              ROUND(MAX(value), 2) AS max
             FROM measurement
             WHERE measuredPhenomenonId = ${id}
              AND aggregated = FALSE
              AND measureTimestamp < ${lastHourTs}
             GROUP BY
              EXTRACT(HOUR FROM measureTimestamp),
              EXTRACT(DAY FROM measureTimestamp)
             ORDER BY MAX(measureTimestamp)
          """
        .map(measurementFromRs).toList().apply()
    Logger.info(s"Loaded ${toAggregate.size} measures of ${name} to aggregate")
    val updated =
      sql"""
             DELETE FROM measurement
             WHERE measureTimestamp < ${lastHourTs}
             AND aggregated = FALSE
             AND measuredPhenomenonId = ${id}
           """
        .update.apply()
    Logger.info(s"Removed $updated rows")
    if (toAggregate.size > 0) {
      sql"""
            INSERT INTO measurement (value, measureTimestamp, aggregated, measuredPhenomenonId)
            VALUES (?, ?, ?, ?)
            """
        .batch(toAggregate.map(message => Seq(
          message.average,
          new java.sql.Timestamp(message.measureTimestamp.toEpochMilli),
          true,
          id
        )): _*).apply()
    }
  })

  private def measurementFromRs(rs:WrappedResultSet) = Measurement(
    average = rs.double("avg"),
    min = rs.double("min"),
    max = rs.double("max"),
    measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis)
  )
}

object MeasuredPhenomenonSql {
  val fromRs: ((WrappedResultSet, Clock) => MeasuredPhenomenonSql) =
    (rs, clock) => new MeasuredPhenomenonSql(
      name = rs.string("name"),
      unit = rs.string("unit"),
      id = rs.string("id"),
      sensorId = rs.string("sensorId"),
      _clock =  clock
    )

  implicit val writes: Writes[MeasuredPhenomenonSql] = new Writes[MeasuredPhenomenonSql] {
    def writes(mp: MeasuredPhenomenonSql): JsValue = Json.obj(
      "name" -> mp.name,
      "unit" -> mp.unit
    )
  }
}