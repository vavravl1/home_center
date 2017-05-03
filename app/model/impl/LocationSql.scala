package model.impl

import _root_.play.api.libs.functional.syntax._
import _root_.play.api.libs.json._
import model.Location
import scalikejdbc._

/**
  *
  */
case class LocationSql(val address:String, var locationLabel:String) extends Location {
  /**
    * human readable representation of the sensor, e.g. living room
    */
  override def label: String = locationLabel

  /**
    * Set label to this location
    *
    * @param newLabel
    * @return
    */
  override def updateLabel(newLabel: String) = {
    DB.autoCommit(implicit session => {
      sql"""
          UPDATE location SET label = ${newLabel} WHERE address = ${address}
      """
        .update.apply()
      locationLabel = newLabel
    })
  }
}

object LocationSql {
  implicit val writes: Writes[LocationSql] = (
    (JsPath \ "address").write[String] and
      (JsPath \ "label").write[String]
    )(unlift(LocationSql.unapply))

  implicit val reads: Reads[LocationSql] = (
    (JsPath \ "address").read[String] and
      (JsPath \ "label").read[String]
    )(LocationSql.apply _)

  def fromRs(rs:WrappedResultSet):LocationSql = new LocationSql(
    address = rs.string("address"),
    locationLabel = rs.string("label")
  )
}
