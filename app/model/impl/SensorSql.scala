package model.impl

import java.time.Clock

import _root_.play.api.libs.json._
import model.{Measurement, Sensor}
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

  override def addMeasurement(measurement: Measurement, measuredPhenomenonName: String, unit:String): Unit = {
    DB.localTx(implicit session => {
      val mp = measuredPhenomenons
        .find(mp => mp.name == measuredPhenomenonName && mp.sensorId == id)
        .getOrElse(saveMeasuredPhenomenon(measuredPhenomenonName, unit))

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
            SELECT MP.name, MP.unit, MP.id, MP.sensorId
            FROM measuredPhenomenon MP
            WHERE MP.sensorId = ${id}
            """
        .map(MeasuredPhenomenonSql.fromRs(_, clock)).list().apply()
    })
  }

  override def aggregateOldMeasurements(): Unit = {
    measuredPhenomenons.foreach(mp => mp.aggregateOldMeasurements())
  }

  private def saveMeasuredPhenomenon(measuredPhenomenonName: String, unit:String)(implicit session: DBSession):MeasuredPhenomenonSql = {
    sql"""
         INSERT INTO measuredPhenomenon(name, unit, sensorId)
         VALUES (${measuredPhenomenonName}, ${unit}, ${id})
      """.update().apply()
    sql"""
          SELECT MP.name, MP.unit, MP.sensorId, MP.id
          FROM measuredPhenomenon MP
          WHERE MP.name = ${measuredPhenomenonName}
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
