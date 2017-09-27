package model.sensor.impl

import java.time.Clock

import _root_.play.api.libs.json._
import model.location.impl.LocationSql
import model.sensor._
import scalikejdbc.{WrappedResultSet, _}

/**
  * Sql implementation of Sensor
  */
case class SensorSql(
                 override val name: String,
                 override val location: LocationSql,
                 val id: String,
                 val _clock: Clock
               ) extends Sensor {
  implicit val clock = _clock

  override def addMeasurement(measurement: Measurement, measuredPhenomenon:MeasuredPhenomenon): Unit = {
    DB.localTx(implicit session => {
      val mp = measuredPhenomenons
        .find(mp => mp.name == measuredPhenomenon.name && mp.sensorId == id)
        .getOrElse(saveMeasuredPhenomenon(
          measuredPhenomenon.name,
          measuredPhenomenon.unit,
          measuredPhenomenon.aggregationStrategy
        ))

      sql"""
          INSERT INTO measurement (value, measureTimestamp, measuredPhenomenonId, aggregated)
          VALUES (
            ${measurement.average},
            ${measurement.measureTimestamp},
            ${mp.id},
            FALSE
          )
      """
        .update.apply()
    })
  }

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
  override def findOrCreatePhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy): MeasuredPhenomenon = {
    DB.localTx(implicit session => {
      return measuredPhenomenons
        .find(mp => mp.name == name && mp.sensorId == id)
        .getOrElse(saveMeasuredPhenomenon(name, unit, aggregationStrategy))
    })
  }

  private def saveMeasuredPhenomenon(name: String, unit:String, aggregationStrategy: MeasurementAggregationStrategy)(implicit session: DBSession):MeasuredPhenomenonSql = {
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

  implicit val writes = new Writes[SensorSql] {
    def writes(s: SensorSql): JsValue = {
      Json.obj(
        "name" -> s.name,
        "location" -> Json.toJson(s.location)(LocationSql.writes),
        "measuredPhenomenons" -> Json.toJson(s.measuredPhenomenons)
      )
    }
  }
}
