package model

/**
  * Repository for Locations
  */
trait LocationRepository {
  def findLocation(address:String):Option[Location]
  def findOrCreateLocation(address: String, label:String):Location
}
