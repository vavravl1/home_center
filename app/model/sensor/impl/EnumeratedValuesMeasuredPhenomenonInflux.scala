package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import dao._
import loader.{ForeverRetentionPolicy, OneHourRetentionPolicy, RetentionPolicy}
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class EnumeratedValuesMeasuredPhenomenonInflux(
                                                override val name: String,
                                                override val unit: String,
                                                override val aggregationStrategy: MeasurementAggregationStrategy,
                                                override val sensor: SensorSql,
                                                val clock: Clock,
                                                val influx: Database) extends MeasuredPhenomenonInflux(name, unit, aggregationStrategy, sensor) {
  override def addMeasurement(measurement: Measurement): Unit = {
    val point = Point(key = key, timestamp = measurement.measureTimestamp.getEpochSecond)
      .addTag("phenomenon", name)
      .addField("value", measurement.average.asInstanceOf[String])

    val f = influx.write(
      point = point,
      precision = Precision.SECONDS,
      consistency = Consistency.ONE,
      retentionPolicy = ForeverRetentionPolicy.toString
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
    val query = queryInflux(lastMeasureTimestamp)

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "value", "value", "value", _)
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


