package controllers

import com.mohiva.play.silhouette.api.{HandlerResult, Silhouette}
import dao.{TimeGranularity, WateringDao}
import entities.watering._
import mqtt.watering.WateringCommander
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import security.utils.auth.DefaultEnv

import scala.concurrent.Future

class WateringController(
                          wateringDao: WateringDao,
                          wateringCommander: WateringCommander,
                          silhouette: Silhouette[DefaultEnv]
                        ) extends Controller {

  def getActualState = Action.async {
    Future {
      Ok(Json.toJson(wateringDao.getLastMessage()))
    }
  }

  def getAllStates(timeGranularity: String) = Action.async {
    Future {
      Ok(Json.toJson(wateringDao.getAveragedMessages(
        TimeGranularity.parse(timeGranularity, false)
      )))
    }
  }

  def manualWatering() = silhouette.SecuredAction.async { implicit request =>
    if(request.identity.admin) {
      Logger.info("Manual watering initiated")
      wateringCommander.sendCommand(WateringCommand(Set(ManualWateringCommand())))
        .map(_ => NoContent)
    } else {
      Future.successful(Unauthorized)
    }
  }
}

