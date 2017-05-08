package model.impl

import java.time.Clock

import model.{Sensor, SensorRepository}
import scalikejdbc._

/**
  * Sql implementation of sensor repository
  */
class SensorRepositorySql(
                           locationRepository: LocationRepositorySql,
                           val clock: Clock) extends SensorRepository {

  override def findOrCreateSensor(locationAddress: String, name: String, measuredPhenomenon: String, unit: String): Sensor = {

    DB.localTx(implicit session => {
      val maybeSensorSql: Option[Sensor] = getSensor(locationAddress, measuredPhenomenon)

      if (maybeSensorSql.isEmpty) {
        storeSensor(locationAddress, name, measuredPhenomenon, unit)
        getSensor(locationAddress, measuredPhenomenon).get
      } else {
        maybeSensorSql.get
      }
    })
  }

  override def find(locationAddress: String, measuredPhenomenon: String): Option[Sensor] = DB.autoCommit(implicit session => {
    getSensor(locationAddress, measuredPhenomenon)
  })

  override def findAll(): Seq[Sensor] = DB.autoCommit(implicit session => {
    sql"""
           SELECT S.id, S.name, S.measuredPhenomenon, S.unit, L.address, L.label
           FROM sensor S
           JOIN location L ON S.location_address = L.address
           ORDER BY L.address
        """
      .map(rs => SensorSql.fromRs(rs, clock)).list().apply()
  })

  override def delete(sensor:Sensor): Unit =
    DB.localTx(implicit session => {
      sql"""
            DELETE FROM sensor
            WHERE location_address = ${sensor.location.address} AND measuredPhenomenon = ${sensor.measuredPhenomenon}
        """.update().apply()
    })

  private def getSensor(locationAddress: String, measuredPhenomenon: String)(implicit session: DBSession): Option[Sensor] = {
    sql"""
           SELECT S.id, S.name, S.measuredPhenomenon, S.unit, L.address, L.label
           FROM sensor S
           JOIN location L ON S.location_address = L.address
           WHERE S.location_address = ${locationAddress} AND S.measuredPhenomenon = ${measuredPhenomenon}
        """
      .map(rs => SensorSql.fromRs(rs, clock)).single().apply()
  }

  private def storeSensor(locationAddress: String, name: String, measuredPhenomenon: String, unit: String)(implicit session: DBSession) = {
    sql"""
              INSERT INTO sensor(name, measuredPhenomenon, unit, location_address)
              VALUES (${name}, ${measuredPhenomenon}, ${unit}, ${locationAddress})
          """.update.apply()
  }
}
