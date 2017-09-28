package loader

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.wire
import config.HomeControllerConfiguration
import mqtt._
import mqtt.clown.{BigClownStoringListener, MqttBigClownParser}
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
  lazy val bcBridgeListenerActor: ActorRef = actorSystem.actorOf(Props(wire[BigClownStoringListener]))
  lazy val mqttRepeatersActors = prepareRepeatingMqttClient

  def initializeListeners(): Unit = {
    mqttConnector.reconnect.run()

    bcBridgeListenerActor ! MqttListenerMessage.Ping
    mqttRepeatersActors.foreach(mqttRepeaterActor => mqttRepeaterActor ! MqttListenerMessage.Ping)

    mqttDispatchingListener.addListener(bcBridgeListenerActor.path)
    mqttRepeatersActors.foreach(mqttRepeaterActor => mqttDispatchingListener.addListener(mqttRepeaterActor.path))
  }


  private def prepareRepeatingMqttClient:Seq[ActorRef] = {
    val remoteMqttUrl = configuration.getString("home_center.mqtt_repeater.url")
    val remoteMqttClientId = configuration.getString("home_center.mqtt_repeater.url")

    if(!remoteMqttClientId.isDefined || !remoteMqttClientId.isDefined) {
      return Seq()
    }

    val remoteMqttConnector = new MqttConnector(
      HomeControllerConfiguration(
        remoteMqttUrl.get,
        remoteMqttClientId.get
      ),
      new RepeatingMqttCallback(mqttConnector),
      actorSystem
    )

    val mqttRepeaterProps = Props(
      new MqttRepeater(
        actorSystem = actorSystem,
        localMqttConnector = mqttConnector,
        remoteMqttConnector = remoteMqttConnector,
        clock = clock
      )
    )
    return (1 to 4).map(i => actorSystem.actorOf(mqttRepeaterProps))
  }
}

