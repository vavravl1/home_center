package loader

import com.paulgoldbaum.influxdbclient.InfluxDB
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._

trait InfluxDbConfig extends BuiltInComponents {
  lazy val influxDbName = configuration.getString("home_center.influx.database")

  lazy val influxdb = InfluxDB.connect(
    configuration.getString("home_center.influx.address").getOrElse("192.168.100.251"),
    configuration.getInt("home_center.influx.port").getOrElse(8086)
  )
  lazy val influx = influxdb.selectDatabase(
    influxDbName.getOrElse("home_center")
  )

  influx.exists()
    .filter(exists => !exists)
    .foreach(_ => {
      influx.create().onSuccess({ case _ => influx.query(
          s"CREATE RETENTION POLICY two_days " +
            s"ON $influxDbName " +
            s"DURATION 2d REPLICATION 1 DEFAULT"
        )
      })
    })
}
