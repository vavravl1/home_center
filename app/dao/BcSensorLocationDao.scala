package dao

import entities.bigclown.BcSensorLocation
import scalikejdbc.{DB, _}

/**
  * Dao for BcSensorLocation(s)
  */
class BcSensorLocationDao {
  def delete(location: String) = {
    DB.autoCommit(implicit session => {
      sql"""DELETE FROM bc_sensor_location WHERE location = ${location}""".update().apply()
    })
  }

  def saveOrUpdate(location: BcSensorLocation): Unit = {
    DB.autoCommit(implicit session => {
      sql"""SELECT location FROM bc_sensor_location WHERE location = ${location.location}"""
        .map(rs => 1).toList().apply() match {
        case Nil =>
          sql"""
            INSERT INTO bc_sensor_location (location, label)
            VALUES (
                ${location.location},
                ${location.label}
            )
          """.update.apply()
        case _ :: _ =>
          sql"""
            UPDATE bc_sensor_location
            SET label = ${location.label}
            WHERE location = ${location.location}
          """.update.apply()
      }
    })
  }

  def getAllLocations(): Seq[BcSensorLocation] =
    DB.readOnly(implicit session => {
      sql"""SELECT location, label FROM bc_sensor_location ORDER BY location"""
        .map(rs => {
          BcSensorLocation(
            label = rs.string("label"),
            location = rs.string("location")
          )
        }).toList().apply()
    })
}
