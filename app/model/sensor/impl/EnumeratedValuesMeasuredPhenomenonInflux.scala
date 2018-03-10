package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.{Database, Point}
import dao._
import loader.ForeverRetentionPolicy
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class EnumeratedValuesMeasuredPhenomenonInflux(
                                                override val name: String,
                                                override val unit: String,
                                                override val aggregationStrategy: MeasurementAggregationStrategy,
                                                override val sensor: SensorSql,
                                                val clock: Clock,
                                                override val influx: Database) extends MeasuredPhenomenonInflux(name, unit, aggregationStrategy, sensor, influx) {
  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average.asInstanceOf[String])
    storePoint(point, measurement, ForeverRetentionPolicy)
  }

  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (_, _, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux(clock)
    val query = queryInflux(lastMeasureTimestamp)

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "value", "value", "value", _.toString)
    })
  }

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = {
    val query = s"" +
      s"SELECT value " +
      s"FROM $ForeverRetentionPolicy.$key " +
      s"WHERE phenomenon = '$name' " +
      s"ORDER BY DESC " +
      s"LIMIT $n"

    Logger.debug(s"Querying influx: $query")

    val series = Await.result(influx.query(query), Duration.Inf).series
    seriesToMeasurements(series, "value", "value", "value", _.toString)
  }

  private def queryInflux(lastMeasureTimestamp: Instant) = {
    s"SELECT value " +
      s"FROM ${influx.databaseName}.$ForeverRetentionPolicy.$key " +
      s"WHERE time > '$lastMeasureTimestamp' " +
      s"AND phenomenon = '$name' "
  }
}


