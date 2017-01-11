package controllers

import dao.{TimeGranularity, WateringDao}
import entities.watering._
import mqtt.watering.WateringCommander
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

class WateringController(
                          wateringDao: WateringDao,
                          wateringCommander: WateringCommander
                        ) extends Controller {

  def getActualState = Action.async {
    Future {
      Ok(Json.toJson(wateringDao.getLastMessage()))
    }
  }

  def getAllStates(timeGranularity: String) = Action.async {
    Future {
      Ok(Json.toJson(wateringDao.getAveragedMessages(
        TimeGranularity.parse(timeGranularity)
      )))
    }
  }

  def manualWatering() = Action.async {
    Logger.info("Manual watering initiated")
    wateringCommander.sendCommand(WateringCommand(Set(ManualWateringCommand()))).map(_ => NoContent)
  }
}

