package model.sensor.impl

import java.time.{Clock, Instant}

import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}
import com.paulgoldbaum.influxdbclient.{Database, Point, Series}
import loader.RetentionPolicy
import model.sensor._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scalikejdbc.WrappedResultSet

import scala.util.{Failure, Success, Try}

abstract class MeasuredPhenomenonInflux(
                                         override val name: String,
                                         override val unit: String,
                                         override val aggregationStrategy: MeasurementAggregationStrategy,
                                         val sensor: SensorSql,
                                         val influx: Database
                                       ) extends MeasuredPhenomenon {
  val key: String = "measurements_" + sensor.id

  protected def storePoint(point: Point, measurement: Measurement, retentionPolicy: RetentionPolicy) = {
    val f = influx.write(
      point = point,
      precision = Precision.SECONDS,
      consistency = Consistency.ONE,
      retentionPolicy = retentionPolicy.toString
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

  protected def seriesToMeasurements(
                                      series: List[Series],
                                      mean: String,
                                      min: String,
                                      max: String,
                                      mapper: (String => Any)
                                    ):Seq[Measurement] = {
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
          mapper(r(mean).toString),
          mapper(r(min).toString),
          mapper(r(max).toString),
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
