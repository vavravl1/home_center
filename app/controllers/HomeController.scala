package controllers

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc._
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
class HomeController(actorSystem: ActorSystem,
                     silhouette: Silhouette[DefaultEnv]) extends Controller {

  def indexReact = silhouette.UserAwareAction.async { implicit userAwareRequest => Future {
    userAwareRequest.identity match {
      case Some(user) => Ok(views.html.index(Some(user), CSRFHelper.token(), None))
      case None => Ok(views.html.index(None, CSRFHelper.token()))
    }
  }}
}
