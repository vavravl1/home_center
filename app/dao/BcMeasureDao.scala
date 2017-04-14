package dao

import java.sql.Timestamp
import java.time.temporal.ChronoUnit._
import java.time.{Clock, Instant}

import _root_.play.api.Logger
import entities.bigclown.{AggregatedBcMeasure, BcMeasure, BcSensorCoordinates}
import scalikejdbc._

/**
  * Dao for bc measures
  */
class BcMeasureDao(_clock: Clock) {

  implicit val clock = _clock
  def cleanDb():Unit = {
    DB.autoCommit(implicit session => {
      sql"""
           TRUNCATE TABLE bc_measure;
        """.update().apply()
    })
  }

  def cleanSensor(location:String):Unit = {
    DB.autoCommit(implicit session => {
      sql"""DELETE FROM bc_measure WHERE location=${location}"""
    })
  }

  def save(message: BcMeasure): Unit = {
    DB.autoCommit(implicit session => {
      sql"""
          INSERT INTO bc_measure (location, phenomenon, sensor, measure_timestamp, value, unit)
          VALUES (
              ${message.location},
              ${message.phenomenon},
              ${message.sensor},
              ${new java.sql.Timestamp(message.measureTimestamp.toEpochMilli)},
              ${message.value},
              ${message.unit}
          )
      """
        .update.apply()
    })
  }

  def getSampledMeasures(
                          location: String,
                          phenomenon: String,
                          by: TimeGranularity = ByHour
                        ): Seq[AggregatedBcMeasure] = {
    DB.readOnly(implicit session => {
      val (extractTime, lastMeasureTimestamp) = by.toExtractAndTime

      sql"""
           SELECT
            MAX(measure_timestamp) AS ts, ROUND(AVG(value), 2) AS avg,
            ROUND(MIN(value), 2) as min, ROUND(MAX(value), 2) as max, unit, sensor,
            bc_sensor_location.label AS label
           FROM bc_measure
           JOIN bc_sensor_location ON bc_measure.location = bc_sensor_location.location
           WHERE phenomenon = ${phenomenon} AND bc_measure.location = ${location}
           AND measure_timestamp > ${lastMeasureTimestamp}
           GROUP BY EXTRACT(${extractTime} FROM measure_timestamp), unit, sensor, label
           ORDER BY MAX(measure_timestamp)
        """
        .map(rs => AggregatedBcMeasure(
          location = rs.string("label"),
          sensor = rs.string("sensor"),
          phenomenon = phenomenon,
          measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis),
          min = rs.double("min"),
          max = rs.double("max"),
          average = rs.double("avg"),
          unit = rs.string("unit")
        )).toList().apply()
    })
  }

  def sensorAggregation(): Unit = {
    DB.localTx(implicit session => {
      val lastHour = clock.instant().truncatedTo(HOURS).minus(1, HOURS)
      val lastHourTs = new Timestamp(lastHour.toEpochMilli)
      val aggregated =
        sql"""
           SELECT MAX(measure_timestamp) AS ts, AVG(value) AS val, unit, sensor, phenomenon, location
           FROM bc_measure
           WHERE measure_timestamp < ${lastHourTs}
           AND aggregated = FALSE
           GROUP BY EXTRACT(HOUR FROM measure_timestamp), unit, sensor, phenomenon, location
           ORDER BY MAX(measure_timestamp)
        """
          .map(rs => BcMeasure(
            location = rs.string("location"),
            sensor = rs.string("sensor"),
            phenomenon = rs.string("phenomenon"),
            measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis),
            value = rs.double("val"),
            unit = rs.string("unit")
          )).toList().apply()
      Logger.info(s"Loaded ${aggregated.size} measures to aggregate")
      val updated = sql"""
           DELETE FROM bc_measure
           WHERE measure_timestamp < ${lastHourTs}
           AND aggregated = FALSE
         """
        .update.apply()
      Logger.info(s"Removed $updated rows")
      if (aggregated.size > 0) {
        sql"""
          INSERT INTO bc_measure (sensor, measure_timestamp, value, unit, phenomenon, aggregated, location)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          """
          .batch(aggregated.map(message => Seq(
            message.sensor,
            new java.sql.Timestamp(message.measureTimestamp.toEpochMilli),
            message.value,
            message.unit,
            message.phenomenon,
            true,
            message.location
          )):_*).apply()
      }
    })
  }

  def getAvailableBcSensors():Seq[BcSensorCoordinates] = DB.readOnly(implicit session => {
    sql"""
          SELECT location, phenomenon FROM bc_measure GROUP BY location, phenomenon ORDER BY phenomenon
      """.map(rs => BcSensorCoordinates(rs.string("location"), rs.string("phenomenon")))
      .toList().apply()
  })
}
