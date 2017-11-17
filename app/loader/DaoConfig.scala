package loader

import com.softwaremill.macwire.wire
import model.location.impl.LocationRepositorySql
import model.sensor.SensorRepository
import model.sensor.impl.SensorRepositorySql
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Defines all repositories plus db maintenance
  */
trait DaoConfig extends BuiltInComponents with ClockConfig with SqlH2Config with InfluxDbConfig{
  lazy val locationRepository: LocationRepositorySql = wire[LocationRepositorySql]
  lazy val sensorRepository: SensorRepository = new SensorRepositorySql(locationRepository, clock, influx)

  def initDbAggregation(): Unit = {
    actorSystem.scheduler.schedule(
      10 second,
      1 hour,
      new Runnable {
        override def run() = {
          sensorRepository.findAll().foreach(sensor => sensor.aggregateOldMeasurements())
        }
      }
    )
  }
}
