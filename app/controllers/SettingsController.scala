package controllers

import com.google.common.io.BaseEncoding
import com.mohiva.play.silhouette.api.Silhouette
import dao.BcSensorLocationDao
import model.{Location, LocationRepository}
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
class SettingsController(locationRepo: LocationRepository,
                         dao: BcSensorLocationDao,
                         silhouette: Silhouette[DefaultEnv]) extends Controller {

  def getBcSensorLocation() = Action.async {
    Future {
      Ok(Json.toJson(locationRepo.getAllLocations()))
    }
  }

  def updateBcSensorLocation() = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        val json = request.body.asJson.get
        val locationToUpdate = json.as[Location]
        Logger.info(s"Updating $locationToUpdate")
        locationToUpdate.updateLabel(locationToUpdate.label)
        NoContent
      } else {
        Unauthorized
      }
    }
  }

  def deleteBcSensorLocation(location: String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        dao.delete(new String(BaseEncoding.base64().decode(location)))
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
