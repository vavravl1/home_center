package controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import filters.StatsActor
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
class HomeController(actorSystem: ActorSystem) extends Controller {

  def numberOfRequestsExample = Action.async {
    implicit val timeout = Timeout(5, TimeUnit.MINUTES)
    val requestCountFuture = (
      actorSystem.actorSelection(StatsActor.path) ? StatsActor.GetRequestsCount
      ).mapTo[Int]
    for {
      count <- requestCountFuture
    } yield Ok(Json.toJson(count))
  }

  def indexReact = Action {
    Ok(views.html.index())
  }
}
