package mqtt.listener

import akka.actor.Actor
import model.sensor.{MeasuredPhenomenon, Measurement, Sensor}

/**
  * Class for processing measurements messages using actors
  */
abstract class SensorMeasurementsListener extends Actor {
  override def receive(): Receive = {
    case SensorMeasurementsListenerMessages.Ping => ping
    case SensorMeasurementsListenerMessages.ConsumeMessage(
       sensor: Sensor,
       phenomenon: MeasuredPhenomenon,
       measurement: Measurement
    ) => messageReceived(sensor, phenomenon, measurement)
  }

  /**
    * Initial message when actor is instantiated. Received only once in lifetime
    */
  def ping = ()

  /**
    * Called after a message is received
    */
  def messageReceived(
                       sensor: Sensor,
                       phenomenon: MeasuredPhenomenon,
                       measurement: Measurement
                     ): Unit = {}
}

object SensorMeasurementsListenerMessages {
  case class Ping()
  case class ConsumeMessage(
                             sensor: Sensor,
                             phenomenon: MeasuredPhenomenon,
                             measurement: Measurement
                           )
}
