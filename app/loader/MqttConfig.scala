package loader

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import config.HomeControllerConfiguration
import mqtt.clown.MqttBigClownParser
import mqtt.repeater.{MqttRepeaterLimiter, MqttRepeaterSender}
import mqtt.{MqttListenerMessage, _}
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

  lazy val mqttBigClownParser = wire[MqttBigClownParser]
  lazy val mqttRepeater:Option[ActorRef] = prepareMqttRepeater

  def initializeListeners(): Unit = {
    mqttConnector.reconnect.run()
    mqttRepeater.map(_ ! MqttListenerMessage.Ping)
    mqttRepeater.map(a => mqttDispatchingListener.addListener(a.path))
  }

  private def prepareMqttRepeater():Option[ActorRef] = {
    val remoteMqttUrl = configuration.getString("home_center.mqtt_repeater.url")
    val remoteMqttClientId = configuration.getString("home_center.mqtt_repeater.url")

    if(remoteMqttClientId.isDefined && remoteMqttClientId.isDefined) {
      val remoteMqttConnector = new MqttConnector(
        HomeControllerConfiguration(
          remoteMqttUrl.get,
          remoteMqttClientId.get
        ),
        EmptyMqttCallback,
        actorSystem
      )

      val mqttSenderProps = Props(
        new MqttRepeaterSender(
          remoteMqttConnector = remoteMqttConnector,
          clock = clock
        )
      )

      val senders = (1 to 4).map(_ => actorSystem.actorOf(mqttSenderProps))

      Some(actorSystem.actorOf(Props(
        new MqttRepeaterLimiter(
          clock = clock,
          remoteMqttConnector  = remoteMqttConnector,
          actorSystem = actorSystem,
          senders = senders.map(_.path)
      ))))
    } else {
      None
    }
  }
}
