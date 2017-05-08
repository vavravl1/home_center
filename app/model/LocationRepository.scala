package model

/**
  * Repository for Locations
  */
trait LocationRepository {
  /**
    * Find all locations ordered by their address
    */
  def getAllLocations():Seq[Location]

  /**
    * Find a location by its address
    */
  def findLocation(address:String):Option[Location]

  /**
    * Find or create location by its address. If there is no such location,
    * create a new one with other parameters unspecified
    */
  def findOrCreateLocation(address: String):Location

  /**
    * Delete whole location and all associated sensors
    */
  def deleteLocation(address:String):Unit
}
