package model.ifthen

import akka.actor.Actor
import model.sensor._
import mqtt.MqttListenerMessage.{ConsumeMessage, Ping}
import mqtt.clown.MqttBigClownParser

/**
  * Class responsible for execution of if-thens based on mqtt messages
  */
class MqttIfThenExecutor(
                    mqttBigClownParser: MqttBigClownParser,
                    ifThens: Seq[IfThen]
                  ) extends Actor {
  override def receive(): Receive = {
    case Ping => ()
    case ConsumeMessage(receivedTopic: String, message: String) =>
      mqttBigClownParser.parseMqttMessage(receivedTopic, message)
        .map({case (sensor, phenomenon, measurement) => evaluateMessage(sensor, phenomenon, measurement)})
    case _ =>
  }

  private def evaluateMessage(sensor: Sensor, phenomenon: MeasuredPhenomenon, measurement: Measurement) = {
    ifThens.foreach(ifThen => ifThen.action(sensor, phenomenon, measurement))
  }
}
