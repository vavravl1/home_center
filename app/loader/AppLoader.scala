package loader

import java.time.Clock

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire._
import config.HomeControllerConfiguration
import controllers._
import model.actuator.impl.ActuatorRepositoryNaive
import model.sensor.impl.{LocationRepositorySql, SensorRepositorySql}
import mqtt.clown.BridgeListener
import mqtt.watering.{WateringCommander, WateringHelloListener, WateringListener}
import mqtt.{MqttConnector, MqttDispatchingListener, MqttListenerMessage, MqttRepeater}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.ahc.AhcWSClient
import play.api.routing.Router
import play.filters.csrf.CSRFComponents
import router.Routes
import scalikejdbc.config.DBs
import ws.WattmeterClient

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Main configuration class for home_controller
  */
class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach { configurator =>
      configurator.configure(context.environment)
    }
    createApp(context).application
  }

  def createApp(context: Context) =
    new BuiltInComponentsFromContext(context) with AppComponents
}

trait ClockConfig extends BuiltInComponents {
  lazy val clock = Clock.systemUTC()
}

trait SqlH2Config extends BuiltInComponents with EvolutionsComponents with DBComponents with HikariCPComponents {
  lazy val initDb = {
    applicationEvolutions
    DBs.setupAll()
  }
}

trait DaoConfig extends BuiltInComponents with ClockConfig {
  lazy val locationRepository: LocationRepositorySql = wire[LocationRepositorySql]
  lazy val sensorRepository: SensorRepositorySql = new SensorRepositorySql(locationRepository, clock)

  def initDbAggregation(): Unit = {
    actorSystem.scheduler.schedule(
      10 second,
      1 hour,
      new Runnable {
        override def run() = {
          sensorRepository.findAll().foreach(sensor => sensor.aggregateOldMeasurements())
        }
      }
    )
  }
}

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

  lazy val actuatorRepository: ActuatorRepositoryNaive = wire[ActuatorRepositoryNaive]

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


trait WsClientConfig extends BuiltInComponents with DaoConfig with ClockConfig {
  lazy val wsClient = AhcWSClient()
  lazy val wattmeterClient = wire[WattmeterClient]

  def initWsQuerying(): Unit = {
    actorSystem.scheduler.schedule(
      1 second,
      1 second,
      new Runnable {
        override def run() = {
          wattmeterClient.queryWattmeter()
        }
      }
    )
  }
}


trait FiltersConfig extends BuiltInComponents with CSRFComponents {
  lazy override val httpFilters = Seq(csrfFilter)
}

trait Controllers extends BuiltInComponents with SqlH2Config with SilhouetteAppModule with MqttConfig with WsClientConfig {
  lazy val homeController = wire[HomeController]
  lazy val bigClownController = wire[BigClownController]
  lazy val signinController: SignInController = wire[SignInController]
  lazy val settingsController: SettingsController = wire[SettingsController]
  lazy val actuatorController: ActuatorController = wire[ActuatorController]
}

trait PlayCoreComponents extends BuiltInComponents with Controllers {
  lazy val assets: Assets = wire[Assets]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]
}

trait AppComponents extends BuiltInComponents
  with PlayCoreComponents
  with MqttConfig
  with FiltersConfig
  with SqlH2Config
  with SilhouetteAppModule
  with Controllers {

  Logger.info("The app is about to start")
  initDb
  initDbAggregation()
  initializeListeners()
  initWsQuerying()

  applicationLifecycle.addStopHook(() => {
    mqttConnector.disconnect().map(_ =>
      Future {
        DBs.closeAll()
      }
    )
  })
}

