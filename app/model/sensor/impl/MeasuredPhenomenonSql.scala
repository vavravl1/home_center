package model.sensor.impl

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import _root_.play.Logger
import dao.TimeGranularity
import model.sensor._
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax

/**
  *
  */
case class MeasuredPhenomenonSql(
                                  override val name: String,
                                  override val unit: String,
                                  override val aggregationStrategy: MeasurementAggregationStrategy,
                                  val id: String,
                                  val sensorId: String,
                                  val _clock: Clock
                                ) extends MeasuredPhenomenon {
  implicit val clock = _clock

  override def addMeasurement(measurement: Measurement): Unit = {
    DB.localTx(implicit session => {
      val aggregationLevel = "none"
      sql"""
          INSERT INTO measurement (value, measureTimestamp, measuredPhenomenonId, aggregated)
          VALUES (
            ${measurement.average},
            ${measurement.measureTimestamp},
            ${id},
            ${aggregationLevel}
          )
      """
        .update.apply()
    })
  }

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

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] =
    DB.readOnly(implicit session => {
      sql"""
           SELECT
            measureTimestamp AS ts,
            value AS avg,
            value AS min,
            value AS max
           FROM measurement
           WHERE measuredPhenomenonId = ${id}
           ORDER BY measureTimestamp DESC
           LIMIT ${n}
        """
        .map(measurementFromRs).toList().apply()
    })

  override def aggregateOldMeasurements(): Unit = DB.localTx(implicit session => {
    if (aggregationStrategy == IdentityMeasurementAggregationStrategy) {
      deleteOutliers
    }

    if (aggregationStrategy == SingleValueAggregationStrategy) {
      aggregateSingleValueMeasuredPhenomenon
    } else {
      aggregateNonSingleValueMeasuredPhenomenon
    }
  })

  def deleteOutliers(implicit session: DBSession): Unit = {
    val averageStdDev =
      sql"""
             SELECT AVG(value) AS avg, STDDEV_POP(value) AS stdDev
             FROM measurement
             WHERE measuredPhenomenonId = ${id}
          """
        .map(rs => (rs.double("avg"), rs.double("stdDev"))).single().apply()

    averageStdDev.map({ case (average, stdDev) =>
      Logger.info(s"Standard deviation is ${stdDev} and average is ${average} for ${name}")
      val updated =
        sql"""
             DELETE FROM measurement
             WHERE measuredPhenomenonId = ${id}
             AND ABS(value - (SELECT AVG(value) FROM measurement WHERE measuredPhenomenonId = ${id})) >
                  10*(SELECT STDDEV_POP(value) FROM measurement WHERE measuredPhenomenonId = ${id})
           """
          .update.apply()
      Logger.info(s"Deleted ${updated} outliers from ${name}")
    })
  }


  private def aggregateNonSingleValueMeasuredPhenomenon(implicit session: DBSession): Unit = {
    aggregateNonSingleValueByParams(
      prevAggregationLevel = "none",
      nextAggregationLevel = "byhour",
      aggregateOlderThan = clock.instant().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS),
      sqls"HOUR", sqls"DAY", sqls"DAY"
    )
    aggregateNonSingleValueByParams(
      prevAggregationLevel = "byhour",
      nextAggregationLevel = "byday",
      aggregateOlderThan = clock.instant().truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS),
      sqls"DAY", sqls"MONTH", sqls"YEAR"
    )
  }

  private def aggregateNonSingleValueByParams(
                                               prevAggregationLevel: String,
                                               nextAggregationLevel: String,
                                               aggregateOlderThan: Instant,
                                               aggregationUnit1: SQLSyntax,
                                               aggregationUnit2: SQLSyntax,
                                               aggregationUnit3: SQLSyntax
                                             )(implicit session: DBSession) = {
    val lastHourTs = new Timestamp(aggregateOlderThan.toEpochMilli)
    val toAggregate =
      sql"""
             SELECT
              MAX(measureTimestamp) AS ts,
              AVG(value) AS avg,
              AVG(value) AS min,
              AVG(value) AS max
             FROM measurement
             WHERE measuredPhenomenonId = ${id}
              AND aggregated = ${prevAggregationLevel}
              AND measureTimestamp < ${lastHourTs}
             GROUP BY
              EXTRACT(${aggregationUnit1} FROM measureTimestamp),
              EXTRACT(${aggregationUnit2} FROM measureTimestamp),
              EXTRACT(${aggregationUnit3} FROM measureTimestamp)
             ORDER BY MAX(measureTimestamp)
          """
        .map(measurementFromRs).toList().apply()
    Logger.info(s"Loaded ${toAggregate.size} measures of ${name} to aggregate by ${prevAggregationLevel}")
    deleteNonAggregatedValues(lastHourTs, prevAggregationLevel)
    insertAggregatedMeasurementsToDb(toAggregate, nextAggregationLevel)
  }


  private def aggregateSingleValueMeasuredPhenomenon(implicit session: DBSession): Unit = {
    val aggregationLevel = "none"
    val lastHourTs = new Timestamp(clock.instant.toEpochMilli)
    val toAggregate =
      sql"""
             SELECT
              MAX(measureTimestamp) AS ts,
              MAX(value) AS avg,
              MAX(value) AS min,
              MAX(value) AS max
             FROM measurement
             WHERE measuredPhenomenonId = ${id}
              AND aggregated = ${aggregationLevel}
              AND measureTimestamp < ${lastHourTs}
             GROUP BY
              EXTRACT(DAY FROM measureTimestamp),
              EXTRACT(MONTH FROM measureTimestamp)
             ORDER BY MAX(measureTimestamp)
          """
        .map(measurementFromRs).toList().apply()
    Logger.info(s"Loaded ${toAggregate.size} single value measures of ${name} to aggregate")
    deleteNonAggregatedValues(lastHourTs, "none")
    insertAggregatedMeasurementsToDb(toAggregate, "byhour")
  }

  private def deleteNonAggregatedValues(lastHourTs: Timestamp, aggregationLevel: String)(implicit session: DBSession) = {
    val updated =
      sql"""
             DELETE FROM measurement
             WHERE aggregated = ${aggregationLevel}
             AND measureTimestamp < ${lastHourTs}
             AND measuredPhenomenonId = ${id}
           """
        .update.apply()
    Logger.info(s"Removed $updated rows")
  }

  private def insertAggregatedMeasurementsToDb(toAggregate: List[Measurement], aggregationLevel:String)(implicit session: DBSession) = {
    if (toAggregate.size > 0) {
      sql"""
            INSERT INTO measurement (value, measureTimestamp, aggregated, measuredPhenomenonId)
            VALUES (?, ?, ?, ?)
            """
        .batch(toAggregate.map(message => Seq(
          message.average,
          new java.sql.Timestamp(message.measureTimestamp.toEpochMilli),
          aggregationLevel,
          id
        )): _*).apply()
    }
  }

  private def measurementFromRs(rs: WrappedResultSet) = Measurement(
    average = aggregationStrategy.singleValue(rs.double("avg")),
    min = rs.double("min"),
    max = rs.double("max"),
    measureTimestamp = Instant.ofEpochMilli(rs.timestamp("ts").millis)
  )
}