package model.impl

import java.time.{Clock, Instant}

import _root_.play.api.libs.json._
import dao.TimeGranularity
import model.{AggregatedValues, Measurement, Sensor}
import scalikejdbc._

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
            false,
            ${id}
          )
      """
        .update.apply()
    })
  }

  override def getAggregatedValues(timeGranularity: TimeGranularity): Seq[AggregatedValues] = {
    DB.readOnly(implicit session => {
      val (extractTime, secondExtractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTime

      sql"""
           SELECT
            MAX(measureTimestamp) AS ts,
            ROUND(AVG(value), 2) AS avg,
            ROUND(MIN(value), 2) as min,
            ROUND(MAX(value), 2) as max
           FROM measurement
           WHERE sensor_id = ${id} AND measureTimestamp > ${lastMeasureTimestamp}
           GROUP BY
            EXTRACT(${extractTime} FROM measureTimestamp),
            EXTRACT(${secondExtractTime} FROM measureTimestamp)
           ORDER BY MAX(measureTimestamp)
        """
        .map(rs => AggregatedValues(
          min = rs.double("min"),
          max = rs.double("max"),
          average = rs.double("avg"),
          measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis)
        )).toList().apply()
    })
  }

  override def aggregatedOldMeasurements(): Unit = ???
}

object SensorSql {
  def fromRs(rs:WrappedResultSet):SensorSql = new SensorSql(
    name = rs.string("name"),
    measuredPhenomenon = rs.string("measuredPhenomenon"),
    unit = rs.string("unit"),
    id = rs.string("id"),
    location = new LocationSql(
      rs.string("address"),
      rs.string("label")
    ),
    Clock.systemUTC()
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
