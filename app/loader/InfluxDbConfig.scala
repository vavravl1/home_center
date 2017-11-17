package loader

import com.paulgoldbaum.influxdbclient.InfluxDB
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._

trait InfluxDbConfig extends BuiltInComponents {
  lazy val influxdb = InfluxDB.connect(
    configuration.getString("home_center.influx.address").getOrElse("192.168.100.251"),
    configuration.getInt("home_center.influx.port").getOrElse(8086)
  )
  lazy val influx = influxdb.selectDatabase(
    configuration.getString("home_center.influx.database").getOrElse("home_center")
  )
  influx.exists().map(exists => if(exists == false) influx.create())
}
