package mqtt

/**
  * Implementation of this trait can send json messages to the mqtt
  */
trait JsonSender {
  def send(topic: String, payload: String): Unit
}
