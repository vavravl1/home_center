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

  override def findOrCreateSensor(locationAddress: String, name: String): Sensor = {

    DB.localTx(implicit session => {
      val maybeSensorSql: Option[SensorSql] = getSensor(locationAddress, name)

      if (maybeSensorSql.isEmpty) {
        storeSensor(locationAddress, name)
        getSensor(locationAddress, name).get
      } else {
        maybeSensorSql.get
      }
    })
  }

  override def find(locationAddress: String, name: String): Option[Sensor] = DB.autoCommit(implicit session => {
    getSensor(locationAddress, name)
  })

  override def findAll(): Seq[Sensor] = DB.autoCommit(implicit session => {
    sql"""
          SELECT S.id, S.name, L.address, L.label
          FROM sensor S
          JOIN location L ON S.locationAddress = L.address
          ORDER BY L.address
        """
      .map(rs => SensorSql.fromRs(rs, clock)).list().apply()
  })

  override def delete(sensor: Sensor): Unit =
    DB.localTx(implicit session => {
      sql"""
            DELETE FROM sensor
            WHERE locationAddress = ${sensor.location.address} AND name = ${sensor.name}
        """.update().apply()
    })

  private def getSensor(locationAddress: String, name: String)(implicit session: DBSession): Option[SensorSql] = {
    sql"""
           SELECT S.id, S.name, L.address, L.label
           FROM sensor S
           JOIN location L ON S.locationAddress = L.address
           WHERE S.locationAddress = ${locationAddress} AND S.name = ${name}
        """
      .map(SensorSql.fromRs(_, clock)).single().apply()
  }

  private def storeSensor(locationAddress: String, name: String)(implicit session: DBSession) = {
    sql"""
              INSERT INTO sensor(name, locationAddress)
              VALUES (${name}, ${locationAddress})
          """.update.apply()
  }
}
