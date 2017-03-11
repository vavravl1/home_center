package controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.mohiva.play.silhouette.api.{HandlerResult, Silhouette}
import filters.StatsActor
import play.api.libs.json.Json
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

  def numberOfRequestsExample = Action.async {
    implicit val timeout = Timeout(5, TimeUnit.MINUTES)
    val requestCountFuture = (
      actorSystem.actorSelection(StatsActor.path) ? StatsActor.GetRequestsCount
      ).mapTo[Int]
    for {
      count <- requestCountFuture
    } yield Ok(Json.toJson(count))
  }

  def indexReact = silhouette.UserAwareAction.async { implicit userAwareRequest => Future {
    userAwareRequest.identity match {
      case Some(user) => Ok(views.html.index(Some(user), CSRFHelper.token(), None))
      case None => Ok(views.html.index(None, CSRFHelper.token()))
    }
  }}
}
