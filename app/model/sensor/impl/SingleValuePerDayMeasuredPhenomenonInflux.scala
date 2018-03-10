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

class SingleValuePerDayMeasuredPhenomenonInflux(
                                                 override val name: String,
                                                 override val unit: String,
                                                 override val aggregationStrategy: MeasurementAggregationStrategy,
                                                 override val sensor: SensorSql,
                                                 val clock: Clock,
                                                 val influx: Database
                                               ) extends MeasuredPhenomenonInflux(name, unit, aggregationStrategy, sensor) {
  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average.asInstanceOf[Double])

    val f = influx.write(
      point = point,
      precision = Precision.SECONDS,
      consistency = Consistency.ONE,
      retentionPolicy = OneHourRetentionPolicy.toString
    )

    f.onSuccess {
      case result => Logger.debug(s"Stored $measurement with $result")
    }
    f.onFailure {
      case result => Logger.error(
        s"Failed to store $measurement with $result for sensor $sensor, point is $point",
        result.getCause
      )
    }
  }

  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (_, _, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux(clock)
    val query = queryForSingleValuePhenomenon(timeGranularity, lastMeasureTimestamp)

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "mean", "min", "max")
    })
  }

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = {
    val query = s"" +
      s"SELECT value " +
      s"FROM $OneHourRetentionPolicy.$key " +
      s"WHERE phenomenon = '$name' " +
      s"ORDER BY DESC " +
      s"LIMIT $n"

    Logger.debug(s"Querying influx: $query")

    val series = Await.result(influx.query(query), Duration.Inf).series
    seriesToMeasurements(series, "value", "value", "value")
  }

  private def queryForSingleValuePhenomenon(timeGranularity: TimeGranularity, lastMeasureTimestamp: Instant) = {
    timeGranularity match {
      case ByMonth | ByMonthBig =>
        s"SELECT SUM(max_value) as MAX, SUM(min_value) as MIN, SUM(mean_value) as MEAN " +
          s"FROM ${influx.databaseName}.$ForeverRetentionPolicy.$key " +
          s"WHERE time > '$lastMeasureTimestamp' " +
          s"AND phenomenon = '$name' " +
          s"GROUP BY time(1d) " +
          s"fill(none)"
      case _ =>
        s"SELECT MAX(max_value), MEAN(mean_value), MIN(min_value) " +
          s"FROM ${influx.databaseName}.$ForeverRetentionPolicy.$key " +
          s"WHERE time > '$lastMeasureTimestamp' " +
          s"AND phenomenon = '$name' " +
          s"GROUP BY time(1d) " +
          s"fill(none)"
    }
  }

  private def seriesToMeasurements(
                                    series: List[Series],
                                    mean: String,
                                    min: String,
                                    max: String
                                  ) = {
    if (series.isEmpty) {
      Seq.empty
    } else {
      series.head.records
        .filter(r => Try {
          r(mean) != null && r(min) != null && r(max) != null
        } match {
          case Success(result) => result
          case Failure(_) => false
        })
        .map(r => Measurement(
          r(mean).toString.toDouble,
          r(min).toString.toDouble,
          r(max).toString.toDouble,
          Instant.parse(r("time").toString)
        ))
    }
  }
}


