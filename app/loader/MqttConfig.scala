package loader

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import config.HomeControllerConfiguration
import mqtt.clown.BridgeListener
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
    mqttRepeaterActor ! MqttListenerMessage.Ping

    mqttDispatchingListener.addListener(bcBridgeListenerActor.path)
    mqttDispatchingListener.addListener(mqttRepeaterActor.path)
  }
}

