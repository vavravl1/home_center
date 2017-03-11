package controllers

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
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
class SettingsController(actorSystem: ActorSystem,
                         silhouette: Silhouette[DefaultEnv]) extends Controller {

  def getBcSensorLocation() = Action.async {
    Future {
      Ok(Json.toJson(Seq(
        BcSensorLocation("remote/0", "Upstairs corridor"),
        BcSensorLocation("remote/1", "Bedroom"),
        BcSensorLocation("base/0", "Living room")
      )))
    }
  }

  def updateBcSensorLocation() = silhouette.SecuredAction.async { implicit request =>
    if(request.identity.admin) {
      val json = request.body.asJson.get
      val locationToUpdate = json.as[BcSensorLocation]
      Logger.info(s"Updating $locationToUpdate")
      Future.successful(NoContent)
    } else {
      Future.successful(Unauthorized)
    }
  }
}
