package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import dao.TimeGranularity
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scalikejdbc.WrappedResultSet

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  *
  */
class MeasuredPhenomenonInflux(
                                override val name: String,
                                override val unit: String,
                                override val aggregationStrategy: MeasurementAggregationStrategy,
                                val id: String,
                                val sensorId: String,
                                val _clock: Clock,
                                val influx: Database
                              ) extends MeasuredPhenomenon {
  private implicit val clock = _clock
  private val key = "measurements_" + sensorId

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
      case result => Logger.error(s"Failed to store $measurement with ${result}")
    }
  }
  override def measurements(timeGranularity: TimeGranularity): Seq[Measurement] = {
    val (extractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux
    val query = s"" +
      s"SELECT MAX(value), MEAN(value), MIN(value) " +
      s"FROM $key " +
      s"WHERE time > '$lastMeasureTimestamp' " +
      s"AND phenomenon = '$name' " +
      s"GROUP BY time($extractTime) " +
      s"fill(none)"

    Logger.debug(s"Querying influx: $query")

    val series = Await.result(influx.query(query), Duration.Inf).series
    seriesToMeasurements(series)
  }

  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = {
    val query = s"" +
      s"SELECT MAX(value), MEAN(value), MIN(value) " +
      s"FROM $key " +
      s"AND phenomenon = '$name' " +
      s"LIMIT $n"

    Logger.debug(s"Querying influx: $query")

    val series = Await.result(influx.query(query), Duration.Inf).series
    seriesToMeasurements(series)
  }

  private def seriesToMeasurements(series: List[Series]) = {
    if (series.isEmpty) {
      Seq.empty
    } else {
      series.head.records
        .map(r => Measurement(
          r("mean").toString.toDouble,
          r("min").toString.toDouble,
          r("max").toString.toDouble,
          Instant.parse(r("time").toString)
        ))
    }
  }
}

object MeasuredPhenomenonInflux {
  val fromRs: ((WrappedResultSet, Clock, Database) => MeasuredPhenomenonInflux) =
    (rs, clock, influx) => new MeasuredPhenomenonInflux(
      name = rs.string("name"),
      unit = rs.string("unit"),
      aggregationStrategy = rs.string("aggregationStrategy") match {
        case "boolean" => BooleanMeasurementAggregationStrategy
        case "singleValue" => SingleValueAggregationStrategy
        case "none" => IdentityMeasurementAggregationStrategy
      },
      id = rs.string("id"),
      sensorId = rs.string("sensorId"),
      _clock = clock,
      influx = influx
    )
}
