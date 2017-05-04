package model

import java.time.Instant

import _root_.play.api.libs.json.{Format, Json, _}
import dao.TimeGranularity
import model.impl.SensorSql

/**
  * Represents single sensor, e.g. Big Clown temperature tag.
  * Name and location values must be unique for each sensor, in other words they represents primary key
  */
trait Sensor {
  /**
    * name of the sensor, e.g. barometer
    */
  val name: String

  /**
    * name of the measured phenomenon, e.g. pressure
    */
  val measuredPhenomenon: String

  /**
    * symbol of the measured unit by this sensor, e.g. kPa
    */
  val unit: String

  /**
    *  location of this sensor
    */
  val location: Location

  def addMeasurement(measurement: Measurement)
  def getAggregatedValues(timeGranularity: TimeGranularity):Seq[AggregatedValues]
  def aggregateOldMeasurements()
}

object Sensor {
  implicit val writes: Writes[Sensor] =
    new Writes[Sensor]{
      def writes(o: Sensor): JsValue = o match {
        case s: SensorSql => SensorSql.writes.writes(s)
      }
    }
}

/**
  * Represents aggregated value for a period of time
  * @param min
  * @param max
  * @param average
  */
case class AggregatedValues(
                           val min:Double,
                           val max:Double,
                           val average:Double,
                           val measureTimestamp:Instant
                           )
object AggregatedValues {
  implicit val format: Format[AggregatedValues] = Json.format[AggregatedValues]
}
