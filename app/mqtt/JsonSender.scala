package mqtt

import play.api.libs.json.Writes

import scala.concurrent.Future

/**
  * Implementation of this trait can send json messages to the mqtt
  */
trait JsonSender {
  def send[T](topic: String, payload: T)(implicit writes: Writes[T]): Future[Unit]
}
