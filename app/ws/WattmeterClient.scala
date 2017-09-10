package ws

import mqtt.JsonSender
import play.api.Logger
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
class WattmeterClient(
                       ws: WSClient,
                       jsonSender: JsonSender
                     ) {

  val request: WSRequest = ws.url("http://192.168.100.240/meas.xml")

  def queryWattmeter():Unit = {
    request.get().map(response => {
      val l1Power = (response.xml \ "I1" \ "P").text
      val l2Power = (response.xml \ "I2" \ "P").text
      val l3Power = (response.xml \ "I3" \ "P").text
      val boiler = (response.xml \ "O1" \ "P").text

      Logger.debug(s"Received response from wattmeter L1:${l1Power} W, L2: ${l2Power} W, L3: ${l3Power} W")

      jsonSender.sendRaw("node/main-switchboard/wattrouter/-/L1", l1Power)
      jsonSender.sendRaw("node/main-switchboard/wattrouter/-/L2", l2Power)
      jsonSender.sendRaw("node/main-switchboard/wattrouter/-/L3", l3Power)
      jsonSender.sendRaw("node/main-switchboard/wattrouter/-/boiler", boiler)
    })
  }

}
