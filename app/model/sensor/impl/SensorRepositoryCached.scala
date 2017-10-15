package model.sensor.impl

import model.location.Location
import model.sensor.{Sensor, SensorRepository}

import scala.collection.mutable

/**
  * Sql implementation of sensor repository
  */
class SensorRepositoryCached(
                            underlying: SensorRepositorySql
                           ) extends SensorRepository {
  var cachedSensors:mutable.Map[(Location,String), SensorCached] = mutable.Map.empty
  initialize

  override def findOrCreateSensor(location: Location, name: String): Sensor = this.synchronized {
    cachedSensors.get((location, name))
      .getOrElse({
        val nonCached = underlying.findOrCreateSensor(location, name)
        val cached = new SensorCached(nonCached)
        cachedSensors.put((location, name), cached)
        cached
      })
  }

  override def find(location: Location, name: String): Option[Sensor] = this.synchronized {
    cachedSensors.get((location, name))
  }
  override def findAll(): Seq[Sensor] = this.synchronized {
    cachedSensors.values.toList.sortBy(_.location.address)
  }
  override def delete(sensor: Sensor): Unit = this.synchronized {
    underlying.delete(sensor)
    initialize
  }

  private def initialize = {
    underlying.findAll()
      .foreach(sensor => cachedSensors.put((sensor.location, sensor.name), new SensorCached(sensor)))
  }
}
