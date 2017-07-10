package controllers

import com.mohiva.play.silhouette.api.Silhouette
import model.LocationRepository
import model.actuator.{ActuatorRepository, Command}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
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

  def execute(locationAddress: String, actuatorName: String): Action[AnyContent] = Action.async {
    implicit request => Future {
      Logger.info(s"Command received ${request.body}")
      val json = request.body.asJson.get
      val command = json.as[Command]

      actuatorRepository
        .find(locationRepository.findOrCreateLocation(locationAddress), actuatorName)
        .foreach(actuator => actuator.execute(command))
      NoContent
    }
  }
}
