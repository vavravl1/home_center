package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import dao.TimeGranularity
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
  private val key = "measurements_" + sensor.id

  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average)
    val f = influx.write(
      point = point,
      precision = Precision.SECONDS,
      consistency = Consistency.ONE
    )

    f.onSuccess {
      case result => Logger.debug(s"Stored $measurement with $result")
    }
    f.onFailure {
      case result => Logger.error(s"Failed to store $measurement with $result for sensor $sensor")
    }
  }
  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (extractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux
    val query = s"" +
      s"SELECT MAX(value), MEAN(value), MIN(value) " +
      s"FROM $key " +
      s"WHERE time > '$lastMeasureTimestamp' " +
      s"AND phenomenon = '$name' " +
      s"GROUP BY time($extractTime) " +
      s"fill(none)"

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "mean", "min", "max")
    })
  }

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = {
    val query = s"" +
      s"SELECT value " +
      s"FROM $key " +
      s"WHERE phenomenon = '$name' " +
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