package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import dao._
import loader.{ForeverRetentionPolicy, FourDaysRetentionPolicy, OneHourRetentionPolicy, RetentionPolicy}
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scalikejdbc.WrappedResultSet

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class MeasuredPhenomenonInflux(
                                override val name: String,
                                override val unit: String,
                                override val aggregationStrategy: MeasurementAggregationStrategy,
                                val id: String,
                                val sensor: SensorSql,
                                val _clock: Clock,
                                val influx: Database
                              ) extends MeasuredPhenomenon {
  private implicit val clock = _clock
  val key:String = "measurements_" + sensor.id

  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average)
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
      case result => Logger.error(s"Failed to store $measurement with $result for sensor $sensor")
    }
  }
  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (retentionPolicy, extractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux
    val query = if (aggregationStrategy != SingleValueAggregationStrategy) {
      queryForNonSingleValuePhenomenon(retentionPolicy, extractTime, lastMeasureTimestamp)
    } else {
      queryForSingleValuePhenomenon(timeGranularity, lastMeasureTimestamp)
    }

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

  private def seriesToMeasurements(series: List[Series], mean: String, min: String, max:String) = {
    if (series.isEmpty) {
      Seq.empty
    } else {
      series.head.records
        .filter(r => r(mean) != null && r(min) != null && r(max) != null)
        .map(r => Measurement(
          r(mean).toString.toDouble,
          r(min).toString.toDouble,
          r(max).toString.toDouble,
          Instant.parse(r("time").toString)
        ))
    }
  }
}

object MeasuredPhenomenonInflux {
  val fromRs: ((WrappedResultSet, Clock, Database, SensorSql) => MeasuredPhenomenonInflux) =
    (rs, clock, influx, sensor) => new MeasuredPhenomenonInflux(
      name = rs.string("name"),
      unit = rs.string("unit"),
      aggregationStrategy = rs.string("aggregationStrategy") match {
        case "boolean" => BooleanMeasurementAggregationStrategy
        case "singleValue" => SingleValueAggregationStrategy
        case "none" => IdentityMeasurementAggregationStrategy
      },
      id = rs.string("id"),
      sensor = sensor,
      _clock = clock,
      influx = influx
    )
}
