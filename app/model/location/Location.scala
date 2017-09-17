package model.location

import model.location.impl.LocationSql
import play.api.libs.functional.syntax._
import play.api.libs.json._


/**
  * Location where the sensor is located
  *
  */
trait Location {
  /**
    * representation of this location in the system, e.g. /remote/0
    */
  val address: String

  /**
    * human readable representation of the sensor, e.g. living room
    */
  val label: String

  /**
    * Set label to this location
    * @param newLabel
    * @return
    */
  def updateLabel(newLabel:String):Location

  override def equals(obj: scala.Any): Boolean = {
    if(!obj.isInstanceOf[Location]) {
      return false
    } else {
      val other = obj.asInstanceOf[Location]
      return other.address.equals(this.address)
    }
  }
}

object Location {
  implicit val writes: Writes[Location] =
    new Writes[Location]{
      def writes(o: Location): JsValue = o match {
        case s: LocationSql => LocationSql.writes.writes(s)
      }
    }

  implicit val reads: Reads[Location] = (
    (JsPath \ "address").read[String] and
      (JsPath \ "label").read[String]
    )(LocationSql.apply _)
}