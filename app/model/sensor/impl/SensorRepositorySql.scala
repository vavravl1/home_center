package model.sensor.impl

import java.time.Clock

import model.location.Location
import model.location.impl.LocationRepositorySql
import model.sensor.{Sensor, SensorRepository}
import scalikejdbc._

/**
  * Sql implementation of sensor repository
  */
class SensorRepositorySql(
                           locationRepository: LocationRepositorySql,
                           val clock: Clock) extends SensorRepository {

  override def findOrCreateSensor(location: Location, name: String): SensorSql = {

    DB.localTx(implicit session => {
      val maybeSensorSql: Option[SensorSql] = getSensor(location, name)

      if (maybeSensorSql.isEmpty) {
        storeSensor(location, name)
        getSensor(location, name).get
      } else {
        maybeSensorSql.get
      }
    })
  }

  override def find(location: Location, name: String): Option[Sensor] = DB.autoCommit(implicit session => {
    getSensor(location, name)
  })

  override def findAll(): Seq[SensorSql] = DB.autoCommit(implicit session => {
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

  private def getSensor(location: Location, name: String)(implicit session: DBSession): Option[SensorSql] = {
    sql"""
           SELECT S.id, S.name, L.address, L.label
           FROM sensor S
           JOIN location L ON S.locationAddress = L.address
           WHERE S.locationAddress = ${location.address} AND S.name = ${name}
        """
      .map(SensorSql.fromRs(_, clock)).single().apply()
  }

  private def storeSensor(location: Location, name: String)(implicit session: DBSession) = {
    sql"""
              INSERT INTO sensor(name, locationAddress)
              VALUES (${name}, ${location.address})
          """.update.apply()
  }
}
