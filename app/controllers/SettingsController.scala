package controllers

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import entities.bigclown.BcSensorLocation
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

  def getBcSensorLocationSettings() = Action.async {
    Future {
      Ok(Json.toJson(Seq(
        BcSensorLocation("remote/0", "Upstairs corridor"),
        BcSensorLocation("remote/1", "Bedroom"),
        BcSensorLocation("base/0", "Living room")
      )))
    }
  }
}
