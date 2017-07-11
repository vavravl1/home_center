package loader

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import config.HomeControllerConfiguration
import model.actuator.impl.ActuatorRepositoryNaive
import mqtt.clown.BridgeListener
import mqtt.watering.{WateringCommander, WateringHelloListener, WateringListener}
import mqtt.{MqttConnector, MqttDispatchingListener, MqttListenerMessage, MqttRepeater}
import play.api.BuiltInComponents

/**
  * All mqtt related logic
  */
trait MqttConfig extends BuiltInComponents with DaoConfig with ClockConfig {
  lazy val mqttDispatchingListener: MqttDispatchingListener = wire[MqttDispatchingListener]
  lazy val mqttConnector = new MqttConnector(
    HomeControllerConfiguration(
      mqttBrokerUrl = configuration.getString("home_center.mqtt.url").get,
      mqttClientId = configuration.getString("home_center.mqtt.clientId").get
    ),
    mqttDispatchingListener,
    actorSystem
  )
  lazy val wateringCommander = wire[WateringCommander]

  lazy val actuatorRepository: ActuatorRepositoryNaive = wire[ActuatorRepositoryNaive]

  lazy val wateringListener: ActorRef = actorSystem.actorOf(Props(wire[WateringListener]))
  lazy val wateringHelloListener: ActorRef = actorSystem.actorOf(Props(wire[WateringHelloListener]))
  lazy val bcBridgeListenerActor: ActorRef = actorSystem.actorOf(Props(wire[BridgeListener]))
  lazy val mqttRepeaterActor: ActorRef = actorSystem.actorOf(Props(
    new MqttRepeater(
      HomeControllerConfiguration(
        configuration.getString("home_center.mqtt_repeater.url").orNull,
        configuration.getString("home_center.mqtt_repeater.clientId").orNull
      ),
      actorSystem,
      mqttConnector
    )
  ))

  def initializeListeners(): Unit = {
    mqttConnector.reconnect.run()

    bcBridgeListenerActor ! MqttListenerMessage.Ping
    wateringListener ! MqttListenerMessage.Ping
    wateringHelloListener ! MqttListenerMessage.Ping
    mqttRepeaterActor ! MqttListenerMessage.Ping

    mqttDispatchingListener.addListener(bcBridgeListenerActor.path)
    mqttDispatchingListener.addListener(wateringListener.path)
    mqttDispatchingListener.addListener(wateringHelloListener.path)
    mqttDispatchingListener.addListener(mqttRepeaterActor.path)
  }
}

