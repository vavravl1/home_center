package model.sensor.impl

import model.location.impl.LocationSql
import model.sensor._

/**
  * Sql implementation of Sensor
  */
case class SensorCached(
                         underlying: SensorSql
                       ) extends Sensor {
  override val name = underlying.name
  override val location: LocationSql = underlying.location

  var measuredPhenomenonsCache: Seq[MeasuredPhenomenonCached] = Seq.empty
  var areAllMeasuredPhenomenonsSingleValueCache: Boolean = false
  initialize

  override def addMeasurement(measurement: Measurement, measuredPhenomenon: MeasuredPhenomenon): Unit = {
    underlying.addMeasurement(measurement, measuredPhenomenon)
    initialize
  }

  override def measuredPhenomenons: Seq[MeasuredPhenomenonCached] = measuredPhenomenonsCache

  override def aggregateOldMeasurements(): Unit = {
    underlying.aggregateOldMeasurements()
    initialize
  }

  override def findOrCreatePhenomenon(name: String, unit: String, aggregationStrategy: MeasurementAggregationStrategy): MeasuredPhenomenon = {
    measuredPhenomenonsCache
      .find(mp => mp.name == name && mp.underlying.sensorId == underlying.id)
      .getOrElse({
        val phenomenon = underlying.findOrCreatePhenomenon(name, unit, aggregationStrategy)
        initialize
        phenomenon
      })
  }

  override def areAllMeasuredPhenomenonsSingleValue: Boolean = areAllMeasuredPhenomenonsSingleValueCache

  private def initialize = this.synchronized {
    measuredPhenomenonsCache = underlying.measuredPhenomenons.map(MeasuredPhenomenonCached(_))
    areAllMeasuredPhenomenonsSingleValueCache = underlying.areAllMeasuredPhenomenonsSingleValue
  }
}



