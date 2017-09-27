package loader

import com.softwaremill.macwire.wire
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.ahc.AhcWSClient
import ws.{SolarEdgeClient, WattrouterClient}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  *
  */
trait WsClientConfig extends BuiltInComponents with DaoConfig with ClockConfig with MqttConfig {
  lazy val wsClient = AhcWSClient()
  lazy val wattmeterClient = wire[WattrouterClient]
  lazy val solarEdgeClient = new SolarEdgeClient(
    ws = wsClient,
    sensorRepository = sensorRepository,
    locationRepository = locationRepository,
    clock = clock,
    jsonSender = mqttConnector,
    apiKey = configuration.getString("home_center.solar_edge.apiKey").orNull,
    siteId = configuration.getString("home_center.solar_edge.siteId").orNull
  )

  def initWsQuerying(): Unit = {
    actorSystem.scheduler.schedule(
      1 second,
      1 second,
      new Runnable {
        override def run() = {
          wattmeterClient.queryMeasurements()
        }
      }
    )
    actorSystem.scheduler.schedule(
      1 second,
      5 minute,
      new Runnable {
        override def run() = {
          wattmeterClient.queryStats()
        }
      }
    )
    actorSystem.scheduler.schedule(
      1 second,
      5 minute,
      new Runnable {
        override def run() = {
          solarEdgeClient.querySolarEdge()
        }
      }
    )
  }
}

