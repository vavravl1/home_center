package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import dao._
import loader.{ForeverRetentionPolicy, FourDaysRetentionPolicy, OneHourRetentionPolicy, RetentionPolicy}
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class ManyValuesMeasuredPhenomenonInflux(
                                          override val name: String,
                                          override val unit: String,
                                          override val aggregationStrategy: MeasurementAggregationStrategy,
                                          override val id: String,
                                          override val sensor: SensorSql,
                                          override val _clock: Clock,
                                          override val influx: Database
                                        ) extends MeasuredPhenomenonInflux(
  name,
  unit,
  aggregationStrategy: MeasurementAggregationStrategy,
  id,
  sensor,
  _clock,
  influx

) {
  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (retentionPolicy, extractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux
    val query = queryForNonSingleValuePhenomenon(retentionPolicy, extractTime, lastMeasureTimestamp)

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "mean", "min", "max")
    })
  }

  private def queryForNonSingleValuePhenomenon(retentionPolicy: RetentionPolicy, extractTime: String, lastMeasureTimestamp: Instant) = {
    retentionPolicy match {
      case OneHourRetentionPolicy =>
        s"SELECT MAX(value), MEAN(value), MIN(value) " +
          s"FROM ${influx.databaseName}.$OneHourRetentionPolicy.$key " +
          s"WHERE time > '$lastMeasureTimestamp' " +
          s"AND phenomenon = '$name' " +
          s"GROUP BY time($extractTime) " +
          s"fill(none)"
      case FourDaysRetentionPolicy | ForeverRetentionPolicy =>
        s"SELECT MAX(max_value), MEAN(mean_value), MIN(min_value) " +
          s"FROM ${influx.databaseName}.$retentionPolicy.$key " +
          s"WHERE time > '$lastMeasureTimestamp' " +
          s"AND phenomenon = '$name' " +
          s"GROUP BY time($extractTime) " +
          s"fill(none)"
    }
  }
}


