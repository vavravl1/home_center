package ws

import java.time.{Clock, Instant}

import mqtt.JsonSender
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class WattrouterClient(
                       ws: WSClient,
                       jsonSender: JsonSender,
                       clock: Clock
                     ) {

  val measureRequest: WSRequest = ws.url("http://192.168.100.240/meas.xml")
  val statsRequest: WSRequest = ws.url("http://192.168.100.240/stat_day.xml?day=0")

  def queryMeasurements():Unit = {
    measureRequest.get().map(response => {
      implicit val now = clock.instant
      val l1Power = (response.xml \ "I1" \ "P").text
      val l2Power = (response.xml \ "I2" \ "P").text
      val l3Power = (response.xml \ "I3" \ "P").text
      val boiler = (response.xml \ "O1" \ "P").text

      Logger.debug(s"Received response from wattmeter L1:$l1Power W, L2: $l2Power W, L3: $l3Power W")

      jsonSender.send("node/main-switchboard/wattrouter/-/L1", powerMessage(l1Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/L2", powerMessage(l2Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/L3", powerMessage(l3Power))
      jsonSender.send("node/main-switchboard/wattrouter/-/boiler", powerMessage(boiler))
    })
  }

  def queryStats():Unit = {
    statsRequest.get().map(response => {
      implicit val now = clock.instant
      val surplus = (response.xml \ "SDS1").text
      val l1Cons = (response.xml \ "SDH1").text
      val l2Cons = (response.xml \ "SDH2").text
      val l3Cons = (response.xml \ "SDH3").text

      Logger.debug(s"Received response from wattmeter stats surplus:$surplus kWh, L1 Cons: $l1Cons kWh, L2 Cons: $l2Cons kWh,  L3 Cons: $l3Cons kWh,")

      jsonSender.send("node/main-switchboard/wattrouter-stats/-/L1-surplus", powerMessage(surplus))
      jsonSender.send("node/main-switchboard/wattrouter-stats/-/L1-cons", powerMessage(l1Cons))
      jsonSender.send("node/main-switchboard/wattrouter-stats/-/L2-cons", powerMessage(l2Cons))
      jsonSender.send("node/main-switchboard/wattrouter-stats/-/L3-cons", powerMessage(l3Cons))
    })

  }

  private def powerMessage(power:String)(implicit timestamp:Instant) =
    s"${power},${timestamp.getEpochSecond}"

}
