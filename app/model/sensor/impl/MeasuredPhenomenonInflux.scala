package model.sensor.impl

import java.time.{Clock, Instant}

import akka.stream.FlowMonitorState.Failed
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
import scala.util.{Failure, Success, Try}

abstract class MeasuredPhenomenonInflux(
                                override val name: String,
                                override val unit: String,
                                override val aggregationStrategy: MeasurementAggregationStrategy,
                                val id: String,
                                val sensor: SensorSql,
                                val _clock: Clock,
                                val influx: Database
                              ) extends MeasuredPhenomenon {
  implicit val clock: Clock = _clock
  val key: String = "measurements_" + sensor.id

  val insertRetentionPolicy:RetentionPolicy = OneHourRetentionPolicy

  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average)
    val f = influx.write(
      point = point,
      precision = Precision.SECONDS,
      consistency = Consistency.ONE,
      retentionPolicy = insertRetentionPolicy.toString
    )

    f.onSuccess {
      case result => Logger.debug(s"Stored $measurement with $result")
    }
    f.onFailure {
      case result => Logger.error(s"Failed to store $measurement with $result for sensor $sensor")
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

  protected def seriesToMeasurements(series: List[Series], mean: String, min: String, max: String) = {
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

object MeasuredPhenomenonInflux {
  val fromRs: ((WrappedResultSet, Clock, Database, SensorSql) => MeasuredPhenomenonInflux) =
    (rs, clock, influx, sensor) => rs.string("aggregationStrategy") match {
      case "singleValue" => new SingleValuePerDayMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = SingleValueAggregationStrategy,
        id = rs.string("id"),
        sensor = sensor,
        _clock = clock,
        influx = influx
      )
      case "boolean" => new EnumeratedValuesMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = BooleanMeasurementAggregationStrategy,
        id = rs.string("id"),
        sensor = sensor,
        _clock = clock,
        influx = influx
      )
      case "none" => new DoubleValuesMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = DoubleValuesMeasurementAggregationStrategy,
        id = rs.string("id"),
        sensor = sensor,
        _clock = clock,
        influx = influx
      )
    }
}
