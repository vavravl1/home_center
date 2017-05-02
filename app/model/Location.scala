package model

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
  def label: String

  /**
    * Set label to this location
    * @param newLabel
    * @return
    */
  def updateLabel(newLabel:String)
}