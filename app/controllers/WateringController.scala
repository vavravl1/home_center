package controllers

import com.mohiva.play.silhouette.api.Silhouette
import entities.watering._
import mqtt.watering.WateringCommander
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import security.utils.auth.DefaultEnv

import scala.concurrent.Future

class WateringController(
                          wateringCommander: WateringCommander,
                          silhouette: Silhouette[DefaultEnv]
                        ) extends Controller {
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

