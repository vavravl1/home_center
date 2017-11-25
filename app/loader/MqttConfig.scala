package loader

import akka.actor.{ActorRef, Props}
import config.HomeControllerConfiguration
import mqtt.MqttConnector
import mqtt.clown.MqttBigClownParser
import mqtt.listener.{MqttDispatchingListener, SensorMeasurementsDispatcher, SensorMeasurementsDispatcherMessages}
import mqtt.repeater.{EmptyMqttCallback, MqttRepeater, MqttRepeaterMessage, MqttRepeaterSender}
import play.api.BuiltInComponents

/**
  * All mqtt related logic
  */
trait MqttConfig extends BuiltInComponents with DaoConfig with ClockConfig with IfThenConfig with ThermostatConfig {
  lazy val sensorMeasurementsDispatcher = prepareSensorMeasurementsDispatcher
  lazy val mqttRepeater = prepareMqttRepeater

  private lazy val mqttDispatchingListener: MqttDispatchingListener = new MqttDispatchingListener(
    sensorMeasurementsDispatcher,
    mqttRepeater
  )

  lazy val mqttConnector = new MqttConnector(
    HomeControllerConfiguration(
      mqttBrokerUrl = configuration.getString("home_center.mqtt.url").get,
      mqttClientId = configuration.getString("home_center.mqtt.clientId").get
    ),
    mqttDispatchingListener,
    actorSystem
  )

  lazy val actuatorRepository = prepareActuatorRepository(mqttConnector)

  lazy val thermostatActivator = prepareThermostatActuatorActivator(thermostatSensor, thermostatButton, actuatorRepository.findOrCreateActuator(thermostatLocation, "Relay"))
  lazy val thermostat = prepareThermostat(thermostatActivator)
  lazy val mqttIfThenExecutor = prepareIfThenExecutor(actuatorRepository, Seq(thermostatActivator.ifThen))

  def initializeListeners(): Unit = {
    mqttConnector.reconnect.run()
    sensorMeasurementsDispatcher ! SensorMeasurementsDispatcherMessages.Ping
    mqttRepeater.map(_ ! MqttRepeaterMessage.Ping)
  }

  private def prepareMqttRepeater(): Option[ActorRef] = {
    val remoteMqttUrl = configuration.getString("home_center.mqtt_repeater.url")
    val remoteMqttClientId = configuration.getString("home_center.mqtt_repeater.url")

    if (remoteMqttClientId.isDefined && remoteMqttClientId.isDefined) {
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
        new MqttRepeater(
          clock = clock,
          remoteMqttConnector = remoteMqttConnector,
          actorSystem = actorSystem,
          senders = senders.map(_.path)
        ))))
    } else {
      None
    }
  }

  private def prepareSensorMeasurementsDispatcher(): ActorRef =
    actorSystem.actorOf(Props(
      new SensorMeasurementsDispatcher(
        actorSystem = actorSystem,
        parser = new MqttBigClownParser(
          sensorRepository = sensorRepository,
          locationRepository = locationRepository,
          clock = clock
        ),
        listeners = Seq(
          mqttIfThenExecutor,
          thermostat
        )
      )
    ))
}
