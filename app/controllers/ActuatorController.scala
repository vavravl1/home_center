package controllers

import com.mohiva.play.silhouette.api.Silhouette
import model.LocationRepository
import model.actuator.ActuatorRepository
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Controller for the Actuators
  */
class ActuatorController(
                          locationRepository: LocationRepository,
                          actuatorRepository: ActuatorRepository,
                          silhouette: Silhouette[DefaultEnv]) extends Controller {

  def getAvailableActuators = Action.async {
    Future {
      Ok(Json.toJson(
        actuatorRepository.findAll()
      ))
    }
  }
}
