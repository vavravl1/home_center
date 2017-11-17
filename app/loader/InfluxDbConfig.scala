package loader

import play.api.BuiltInComponents

trait InfluxDbConfig extends BuiltInComponents {
//  lazy val influxdb = InfluxDB.connect("192.168.100.251", 8086)
//  lazy val database = influxdb.selectDatabase("home_center")
//  database.exists().map(exists => if(exists == false) database.create())
}
