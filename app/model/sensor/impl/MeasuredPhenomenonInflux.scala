package model.sensor.impl

import java.time.Clock

import com.paulgoldbaum.influxdbclient.Database
import dao.TimeGranularity
import model.sensor.{MeasuredPhenomenon, Measurement, MeasurementAggregationStrategy}

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
  override def addMeasurement(measurement: Measurement): Unit = ???
  override def measurements(timeGranularity: TimeGranularity): Seq[Measurement] = ???
  override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = ???
  override def aggregateOldMeasurements(): Unit = ???
}
