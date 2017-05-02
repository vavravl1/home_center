package model

import java.time.Instant

/**
  * Represents single measured value of the associated sensor
  *
  * @param value that was measured in the unit specified by the sensor
  * @param measureTimestamp when the measurement of this value happened
  * @param aggregated technical flag that represents that the storage of this value was optimized in the db
  */
case class Measurement(
                 val value: Double,
                 val measureTimestamp: Instant,
                 val aggregated: Boolean
                 )