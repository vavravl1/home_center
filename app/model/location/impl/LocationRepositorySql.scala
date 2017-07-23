package model.location.impl

import model.location.{Location, LocationRepository}
import scalikejdbc._

/**
  *
  */
class LocationRepositorySql extends LocationRepository {
  override def findLocation(address: String): Option[LocationSql] = DB.autoCommit(implicit session => {
      sql"""
           SELECT address, label
           FROM location
           WHERE address = ${address}
        """
        .map(LocationSql.fromRs(_)).single().apply()
    })

  override def getAllLocations(): Seq[Location] = DB.autoCommit(implicit session => {
    sql"""
           SELECT address, label
           FROM location
           ORDER BY address
        """
      .map(LocationSql.fromRs(_)).list().apply()
  })

  override def findOrCreateLocation(address: String): LocationSql = {
    val location = findLocation(address)
    if(location.isEmpty) {
      DB.autoCommit(implicit session => {
        sql"""
          INSERT INTO location (address, label)
          VALUES (
            ${address},
            '???'
          )
      """
          .update.apply()
        new LocationSql(address, "???")
      })
    } else {
      location.get
    }
  }

  /**
    * Delete whole location and all associated sensors
    */
  override def deleteLocation(address: String): Unit =
    DB.localTx(implicit session => {
      sql"""
            DELETE FROM location
            WHERE address = ${address}
        """.update().apply()
    })
}
