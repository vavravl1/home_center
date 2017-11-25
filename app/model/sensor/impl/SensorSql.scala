package model.sensor.impl

import java.time.Clock

import com.paulgoldbaum.influxdbclient.Database
import loader.{ForeverRetentionPolicy, FourDaysRetentionPolicy, OneHourRetentionPolicy}
import model.location.impl.LocationSql
import model.sensor._
import scalikejdbc.{WrappedResultSet, _}

/**
  * Sql implementation of Sensor
  */
case class SensorSql(
                 override val name: String,
                 override val location: LocationSql,
                 id: String,
                 _clock: Clock,
                 influx: Database
               ) extends Sensor {
  private implicit val clock = _clock

  /**
    * All measured phenomenons by this sensor
    */
  override def measuredPhenomenons: Seq[MeasuredPhenomenonInflux] = {
    DB.readOnly(implicit session => {
      sql"""
            SELECT MP.name, MP.unit, MP.id, MP.sensorId, MP.aggregationStrategy
            FROM measuredPhenomenon MP
            WHERE MP.sensorId = ${id}
            ORDER BY MP.name, MP.unit
            """
        .map(MeasuredPhenomenonInflux.fromRs(_, clock, influx, this)).list().apply()
    })
  }

  /**
    * Create or load measured phenomenon according to the given parameters
    */
  override def findOrCreatePhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy): MeasuredPhenomenon = {
    DB.localTx(implicit session => {
      return measuredPhenomenons
        .find(mp => mp.name == name && mp.sensor.id == id)
        .getOrElse(saveMeasuredPhenomenon(name, unit, aggregationStrategy))
    })
  }

  def findPhenomenon(name: String):Option[MeasuredPhenomenon] = {
    DB.localTx(implicit session => {
      return measuredPhenomenons
        .find(mp => mp.name == name && mp.sensor.id == id)
    })
  }

  private def saveMeasuredPhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy)(implicit session: DBSession): MeasuredPhenomenon = {
    val aggregationStrategyName = aggregationStrategy match {
      case IdentityMeasurementAggregationStrategy => "none"
      case SingleValueAggregationStrategy => "singleValue"
      case BooleanMeasurementAggregationStrategy => "boolean"
    }
    sql"""
         INSERT INTO measuredPhenomenon(name, unit, aggregationStrategy, sensorId)
         VALUES (${name}, ${unit}, ${aggregationStrategyName}, ${id})
      """.update().apply()

    val measuredPhenomenon = sql"""
          SELECT MP.name, MP.unit, MP.sensorId, MP.id, MP.aggregationStrategy
          FROM measuredPhenomenon MP
          WHERE MP.name = ${name}
          AND MP.sensorId = ${id}
      """.map(MeasuredPhenomenonInflux.fromRs(_, clock, influx, this)).single().apply().get

    influx.query(
      s"CREATE CONTINUOUS QUERY cq_${FourDaysRetentionPolicy}_${measuredPhenomenon.key} ON ${influx.databaseName} " +
      s"BEGIN " +
      s"SELECT mean(value) AS mean_value, max(value) AS max_value, min(value) AS min_value " +
      s"INTO ${influx.databaseName}.$FourDaysRetentionPolicy.${measuredPhenomenon.key} " +
      s"FROM ${influx.databaseName}.$OneHourRetentionPolicy.${measuredPhenomenon.key} GROUP BY time(${FourDaysRetentionPolicy.downsamplingTime}), phenomenon " +
      s"END"
    )

    influx.query(
      s"CREATE CONTINUOUS QUERY cq_${ForeverRetentionPolicy}_${measuredPhenomenon.key} ON ${influx.databaseName} " +
        s"BEGIN " +
        s"SELECT mean(mean_value) AS mean_value, max(max_value) AS max_value, min(min_value) AS min_value " +
        s"INTO ${influx.databaseName}.$ForeverRetentionPolicy.${measuredPhenomenon.key} " +
        s"FROM ${influx.databaseName}.$FourDaysRetentionPolicy.${measuredPhenomenon.key} GROUP BY time(${ForeverRetentionPolicy.downsamplingTime}), phenomenon " +
        s"END"
    )

    measuredPhenomenon
  }

  override def areAllMeasuredPhenomenonsSingleValue: Boolean =
    DB.readOnly(implicit session => {
      sql"""
           SELECT (
              SELECT count(id) FROM measuredPhenomenon
              WHERE sensorId = ${id}
              AND aggregationStrategy = 'singleValue'
           ) > 0 AND (
              SELECT count(id) FROM measuredPhenomenon
              WHERE sensorId = ${id}
              AND aggregationStrategy != 'singleValue'
           ) = 0 AS allMeasuredPhenomenonsSingleValue

        """.map(rs => rs.boolean("allMeasuredPhenomenonsSingleValue")).single().apply().get
    })
}

object SensorSql {
  val fromRs: ((WrappedResultSet, Clock, Database) => SensorSql) =
    (rs, clock, influx) => SensorSql(
      name = rs.string("name"),
      location = LocationSql(
        address = rs.string("address"),
        label = rs.string("label")
      ),
      rs.string("id"),
      clock,
      influx
    )
}
