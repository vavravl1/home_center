package controllers

import com.mohiva.play.silhouette.api.Silhouette
import model.location.{Location, LocationRepository}
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
        val inputLocation = json.as[Location]
        val locationToUpdate = locationRepo.findOrCreateLocation(inputLocation.address)
        Logger.info(s"Updating $locationToUpdate")
        locationToUpdate.updateLabel(inputLocation.label)
        NoContent
      } else {
        Unauthorized
      }
    }
  }

  def deleteLocation(locationAddress:String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        locationRepo.deleteLocation(locationAddress)
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
