package controllers

import com.mohiva.play.silhouette.api.Silhouette
import model.{Location, LocationRepository, SensorRepository}
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
                         sensorRepository: SensorRepository,
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

  def deleteLocation(location: String, position:String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        locationRepo.deleteLocation(location + "/" + position)
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
