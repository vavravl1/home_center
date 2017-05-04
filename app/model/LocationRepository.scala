package model

/**
  * Repository for Locations
  */
trait LocationRepository {
  def getAllLocations():Seq[Location]
  def findLocation(address:String):Option[Location]
  def findOrCreateLocation(address: String):Location
}
