package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Database
import dao._
import loader.{ForeverRetentionPolicy, RetentionPolicy}
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class EnumeratedValuesMeasuredPhenomenonInflux(
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
  override val insertRetentionPolicy = ForeverRetentionPolicy

  override def measurements(timeGranularity: TimeGranularity): Future[Seq[Measurement]] = {
    val (retentionPolicy, extractTime, lastMeasureTimestamp) = timeGranularity.toExtractAndTimeForInflux
    val query = queryInflux(retentionPolicy, extractTime, lastMeasureTimestamp)

    Logger.debug(s"Querying influx: $query")

    influx.query(query).map(result => {
      seriesToMeasurements(result.series, "value", "value", "value")
    })
  }

  private def queryInflux(retentionPolicy: RetentionPolicy, extractTime: String, lastMeasureTimestamp: Instant) = {
    s"SELECT value " +
      s"FROM ${influx.databaseName}.$ForeverRetentionPolicy.$key " +
      s"WHERE time > '$lastMeasureTimestamp' " +
      s"AND phenomenon = '$name' " +
      s"GROUP BY time($extractTime) " +
      s"fill(none)"
  }
}


