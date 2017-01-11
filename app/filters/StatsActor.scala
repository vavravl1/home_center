package filters

import akka.actor.Actor
import filters.StatsActor.{GetRequestsCount, Ping, RequestReceived}

/**
  *
  */
class StatsActor extends Actor {
  var counter = 0

  override def receive: Receive = {
    case Ping => ()
    case RequestReceived => counter += 1
    case GetRequestsCount => sender() ! counter
  }
}


object StatsActor {
  val name = "statsActor"
  val path = s"/user/$name"

  case object Ping
  case object RequestReceived
  case object GetRequestsCount
}