package loader

import com.softwaremill.macwire.wire
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.ahc.AhcWSClient
import ws.WattmeterClient

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  *
  */
trait WsClientConfig extends BuiltInComponents with DaoConfig with ClockConfig {
  lazy val wsClient = AhcWSClient()
  lazy val wattmeterClient = wire[WattmeterClient]

  def initWsQuerying(): Unit = {
    actorSystem.scheduler.schedule(
      1 second,
      1 second,
      new Runnable {
        override def run() = {
          wattmeterClient.queryWattmeter()
        }
      }
    )
  }
}

