package ws

import java.time.{Clock, Instant}

import mqtt.JsonSender
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class WattmeterClient(
                       ws: WSClient,
                       jsonSender: JsonSender,
                       clock: Clock
                     ) {

  val request: WSRequest = ws.url("http://192.168.100.240/meas.xml")

  def queryWattmeter():Unit = {
    request.get().map(response => {
      implicit val now = clock.instant
      val l1Power = (response.xml \ "I1" \ "P").text
      val l2Power = (response.xml \ "I2" \ "P").text
      val l3Power = (response.xml \ "I3" \ "P").text
      val boiler = (response.xml \ "O1" \ "P").text

      Logger.debug(s"Received response from wattmeter L1:${l1Power} W, L2: ${l2Power} W, L3: ${l3Power} W")

      jsonSender.send("node/main-switchboard/wattrouter/-/L1", powerMessage(l1Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/L2", powerMessage(l2Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/L3", powerMessage(l3Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/boiler", powerMessage(boiler))
    })
  }

  private def powerMessage(power:String)(implicit timestamp:Instant) =
    s"${power},${timestamp.getEpochSecond}"

}
