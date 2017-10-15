package model.sensor.impl

import dao.TimeGranularity
import model.sensor._
import play.api.libs.json.{JsArray, JsValue, Json, Writes}

import scala.collection.mutable

/**
  *
  */
case class MeasuredPhenomenonCached(
                                   underlying: MeasuredPhenomenonSql
                                ) extends MeasuredPhenomenon {

  override val name = underlying.name
  override val unit = underlying.unit
  override val aggregationStrategy = underlying.aggregationStrategy

  var measurementsCache:mutable.Map[TimeGranularity, Seq[Measurement]] = mutable.Map.empty
  var lastNMeasurementsDescendantCache:Seq[Measurement] = Seq.empty

  override def measurements(timeGranularity: TimeGranularity): Seq[Measurement] = this.synchronized {
    measurementsCache.get(timeGranularity)
      .getOrElse({
        val nonCached = underlying.measurements(timeGranularity)
        measurementsCache.put(timeGranularity, nonCached)
        nonCached
      })
  }

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = this.synchronized {
    if(lastNMeasurementsDescendantCache.size >= n) {
      lastNMeasurementsDescendantCache.take(n)
    } else {
      val nonCached = underlying.lastNMeasurementsDescendant(n)
      lastNMeasurementsDescendantCache = nonCached
      nonCached
    }
  }

  override def aggregateOldMeasurements(): Unit = this.synchronized {
    underlying.aggregateOldMeasurements()
    measurementsCache.clear()
    lastNMeasurementsDescendantCache = Seq.empty
  }
}

object MeasuredPhenomenonCached {
  def writesWithMeasurements(timeGranularity: TimeGranularity): Writes[Seq[MeasuredPhenomenonCached]] = new Writes[Seq[MeasuredPhenomenonCached]] {
    def writes(mps: Seq[MeasuredPhenomenonCached]): JsValue =
      JsArray(mps.map(mp => Json.obj(
        "name" -> mp.name,
        "unit" -> mp.unit,
        "measurements" -> mp.measurements(timeGranularity),
        "aggregationStrategy" -> Json.toJson(mp.aggregationStrategy)
      )))
  }
}

