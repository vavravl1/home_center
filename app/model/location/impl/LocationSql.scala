package model.location.impl

import _root_.play.api.libs.functional.syntax._
import _root_.play.api.libs.json._
import model.location.Location
import scalikejdbc._

/**
  *
  */
case class LocationSql(val address:String, val label:String) extends Location {
  /**
    * Set label to this location
    *
    * @param newLabel
    * @return
    */
  override def updateLabel(newLabel: String):Location = {
    DB.autoCommit(implicit session => {
      sql"""
          UPDATE location SET label = ${newLabel} WHERE address = ${address}
      """
        .update.apply()
      LocationSql(address, label)
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
    label = rs.string("label")
  )
}
