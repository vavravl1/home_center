package model.impl

import java.sql.Timestamp
import java.time.temporal.ChronoUnit.HOURS
import java.time.{Clock, Instant}

import _root_.play.api.Logger
import _root_.play.api.libs.json._
import dao.TimeGranularity
import model.{AggregatedValue, Measurement, Sensor}
import scalikejdbc.{WrappedResultSet, _}

/**
  * Sql implementation of Sensor
  */
class SensorSql(
                 override val name: String,
                 override val measuredPhenomenon: String,
                 override val unit: String,
                 private val id: String,
                 override val location: LocationSql,
                 val _clock: Clock
               ) extends Sensor {
  implicit val clock = _clock

  override def addMeasurement(measurement: Measurement): Unit = {
    DB.autoCommit(implicit session => {
      sql"""
          INSERT INTO measurement (value, measureTimestamp, aggregated, sensor_id)
          VALUES (
            ${measurement.value},
            ${measurement.measureTimestamp},
            FALSE,
            ${id}
          )
      """
        .update.apply()
    })
  }

  override def getAggregatedValues(timeGranularity: TimeGranularity): Seq[AggregatedValue] = {
    DB.readOnly(implicit session => {
      val (extractTime, secondExtractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTime

      sql"""
           SELECT
            MAX(measureTimestamp) AS ts,
            ROUND(AVG(value), 2) AS avg,
            ROUND(MIN(value), 2) AS min,
            ROUND(MAX(value), 2) AS max
           FROM measurement
           WHERE sensor_id = ${id} AND measureTimestamp > ${lastMeasureTimestamp}
           GROUP BY
            EXTRACT(${extractTime} FROM measureTimestamp),
            EXTRACT(${secondExtractTime} FROM measureTimestamp)
           ORDER BY MAX(measureTimestamp)
        """
        .map(aggregatedFromRs(_)).toList().apply()
    })
  }

  override def aggregateOldMeasurements(): Unit = DB.localTx(implicit session => {
    val lastHour = clock.instant().truncatedTo(HOURS).minus(1, HOURS)
    val lastHourTs = new Timestamp(lastHour.toEpochMilli)
    val aggregated =
      sql"""
           SELECT
            MAX(measureTimestamp) AS ts,
            ROUND(AVG(value), 2) AS avg,
            ROUND(MIN(value), 2) AS min,
            ROUND(MAX(value), 2) AS max
           FROM measurement
           WHERE sensor_id = ${id} AND aggregated = FALSE
           GROUP BY
            EXTRACT(HOUR FROM measureTimestamp),
            EXTRACT(DAY FROM measureTimestamp)
           ORDER BY MAX(measureTimestamp)
        """
        .map(aggregatedFromRs(_)).toList().apply()
    Logger.info(s"Loaded ${aggregated.size} measures to aggregate")
    val updated =
      sql"""
           DELETE FROM measurement
           WHERE measureTimestamp < ${lastHourTs}
           AND aggregated = FALSE
           AND sensor_id = ${id}
         """
        .update.apply()
    Logger.info(s"Removed $updated rows")
    if (aggregated.size > 0) {
      sql"""
          INSERT INTO measurement (value, measureTimestamp, aggregated, sensor_id)
          VALUES (?, ?, ?, ?)
          """
        .batch(aggregated.map(message => Seq(
          message.average,
          new java.sql.Timestamp(message.measureTimestamp.toEpochMilli),
          true,
          id
        )): _*).apply()
    }
  })

  private val aggregatedFromRs: (WrappedResultSet => AggregatedValue) = rs => {
    AggregatedValue(
      min = rs.double("min"),
      max = rs.double("max"),
      average = rs.double("avg"),
      measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis)
    )
  }
}

object SensorSql {
  val fromRs: ((WrappedResultSet, Clock) => SensorSql) =
    (rs, clock) => new SensorSql(
      name = rs.string("name"),
      measuredPhenomenon = rs.string("measuredPhenomenon"),
      unit = rs.string("unit"),
      id = rs.string("id"),
      location = new LocationSql(
        rs.string("address"),
        rs.string("label")
      ),
      clock
    )

  implicit val writes = new Writes[SensorSql] {
    def writes(s: SensorSql): JsValue = {
      Json.obj(
        "name" -> s.name,
        "measuredPhenomenon" -> s.measuredPhenomenon,
        "unit" -> s.unit,
        "location" -> Json.toJson(s.location)(LocationSql.writes)
      )
    }
  }
}
