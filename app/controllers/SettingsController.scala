package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.BcSensorLocationDao
import entities.bigclown.BcSensorLocation
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
class SettingsController(dao: BcSensorLocationDao,
                         silhouette: Silhouette[DefaultEnv]) extends Controller {

  def getBcSensorLocation() = Action.async {
    Future {
      Ok(Json.toJson(dao.getAllLocations()))
    }
  }

  def updateBcSensorLocation() = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        val json = request.body.asJson.get
        val locationToUpdate = json.as[BcSensorLocation]
        Logger.info(s"Updating $locationToUpdate")
        dao.saveOrUpdate(locationToUpdate)
        NoContent
      } else {
        Unauthorized
      }
    }
  }

  def deleteBcSensorLocation(location: String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        dao.delete(location)
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
