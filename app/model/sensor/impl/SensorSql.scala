package model.sensor.impl

import java.time.Clock

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
                 _clock: Clock
               ) extends Sensor {
  private implicit val clock = _clock

  /**
    * All measured phenomenons by this sensor
    */
  override def measuredPhenomenons: Seq[MeasuredPhenomenonSql] = {
    DB.readOnly(implicit session => {
      sql"""
            SELECT MP.name, MP.unit, MP.id, MP.sensorId, MP.aggregationStrategy
            FROM measuredPhenomenon MP
            WHERE MP.sensorId = ${id}
            ORDER BY MP.name, MP.unit
            """
        .map(MeasuredPhenomenonSql.fromRs(_, clock)).list().apply()
    })
  }

  override def aggregateOldMeasurements(): Unit = {
    measuredPhenomenons.foreach(mp => mp.aggregateOldMeasurements())
  }

  /**
    * Create or load measured phenomenon according to the given parameters
    */
  override def findOrCreatePhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy): MeasuredPhenomenonSql = {
    DB.localTx(implicit session => {
      return measuredPhenomenons
        .find(mp => mp.name == name && mp.sensorId == id)
        .getOrElse(saveMeasuredPhenomenon(name, unit, aggregationStrategy))
    })
  }

  def findPhenomenon(name: String):Option[MeasuredPhenomenon] = {
    DB.localTx(implicit session => {
      return measuredPhenomenons
        .find(mp => mp.name == name && mp.sensorId == id)
    })
  }

  private def saveMeasuredPhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy)(implicit session: DBSession): MeasuredPhenomenonSql = {
    val aggregationStrategyName = aggregationStrategy match {
      case IdentityMeasurementAggregationStrategy => "none"
      case SingleValueAggregationStrategy => "singleValue"
      case BooleanMeasurementAggregationStrategy => "boolean"
    }
    sql"""
         INSERT INTO measuredPhenomenon(name, unit, aggregationStrategy, sensorId)
         VALUES (${name}, ${unit}, ${aggregationStrategyName}, ${id})
      """.update().apply()
    sql"""
          SELECT MP.name, MP.unit, MP.sensorId, MP.id, MP.aggregationStrategy
          FROM measuredPhenomenon MP
          WHERE MP.name = ${name}
          AND MP.sensorId = ${id}
      """.map(MeasuredPhenomenonSql.fromRs(_, clock)).single().apply().get
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
  val fromRs: ((WrappedResultSet, Clock) => SensorSql) =
    (rs, clock) => SensorSql(
      name = rs.string("name"),
      location = LocationSql(
        address = rs.string("address"),
        label = rs.string("label")
      ),
      rs.string("id"),
      clock
    )
}
