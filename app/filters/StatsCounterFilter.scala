package filters

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future

/**
  * Sample filter for demonstration purposes
  */
class StatsCounterFilter(actorSystem: ActorSystem, implicit val mat: Materializer) extends Filter {
  override def apply(nextFilter: (RequestHeader) => Future[Result])
                    (header: RequestHeader): Future[Result] = {
//    Logger.debug(s"Serving another request: ${header.path}")
    actorSystem.actorSelection(StatsActor.path) ! StatsActor.RequestReceived
    nextFilter(header)
  }
}
