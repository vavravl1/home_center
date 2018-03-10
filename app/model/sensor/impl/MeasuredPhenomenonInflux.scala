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
                                val sensor: SensorSql
                              ) extends MeasuredPhenomenon {
  val key: String = "measurements_" + sensor.id
}

object MeasuredPhenomenonInflux {
  val fromRs: ((WrappedResultSet, Clock, Database, SensorSql) => MeasuredPhenomenonInflux) =
    (rs, clock, influx, sensor) => rs.string("aggregationStrategy") match {
      case "singleValue" => new SingleValuePerDayMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = SingleValueAggregationStrategy,
        sensor = sensor,
        clock = clock,
        influx = influx
      )
      case "boolean" => new EnumeratedValuesMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = EnumeratedMeasurementAggregationStrategy,
        sensor = sensor,
        clock = clock,
        influx = influx
      )
      case "none" => new DoubleValuesMeasuredPhenomenonInflux(
        name = rs.string("name"),
        unit = rs.string("unit"),
        aggregationStrategy = DoubleValuesMeasurementAggregationStrategy,
        sensor = sensor,
        clock = clock,
        influx = influx
      )
    }
}
