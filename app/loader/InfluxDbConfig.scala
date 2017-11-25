package loader

import com.paulgoldbaum.influxdbclient.InfluxDB
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait InfluxDbConfig extends BuiltInComponents {
  lazy val influxDbName = configuration.getString("home_center.influx.database").getOrElse("home_center")

  lazy val influxdb = InfluxDB.connect(
    configuration.getString("home_center.influx.address").getOrElse("192.168.100.251"),
    configuration.getInt("home_center.influx.port").getOrElse(8086)
  )
  lazy val influx = influxdb.selectDatabase(influxDbName)

  def prepareInfluxDatabase():Unit = {
    Await.result(
      influx.exists()
        .filter(exists => !exists)
        .flatMap(_ => influx.create())
        .flatMap(_ => influx.query(
            s"CREATE RETENTION POLICY $OneHourRetentionPolicy " +
            s"ON $influxDbName " +
            s"DURATION 1h REPLICATION 1 DEFAULT"))
        .flatMap(_ => influx.query(
            s"CREATE RETENTION POLICY $FourDaysRetentionPolicy " +
            s"ON $influxDbName " +
            s"DURATION 4d REPLICATION 1"))
        .flatMap(_ => influx.query(
            s"CREATE RETENTION POLICY $ForeverRetentionPolicy " +
            s"ON $influxDbName " +
            s"DURATION 0s REPLICATION 1")),
      Duration.Inf
    )
  }
}

sealed abstract class RetentionPolicy(val retentionPolicyName: String, val downsamplingTime: String) {
  override def toString: String = retentionPolicyName
}

object OneHourRetentionPolicy extends RetentionPolicy("one_hour", "n/a")
object FourDaysRetentionPolicy extends RetentionPolicy("four_days", "1m")
object ForeverRetentionPolicy extends RetentionPolicy("forever", "24h")

