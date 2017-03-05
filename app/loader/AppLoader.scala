package loader

import java.time.Clock

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire._
import config.HomeControllerConfiguration
import controllers._
import dao.{BcMeasureDao, WateringDao}
import filters.StatsActor.Ping
import filters.{StatsActor, StatsCounterFilter}
import mqtt.clown.BridgeListener
import mqtt.watering.{WateringCommander, WateringHelloListener, WateringListener}
import mqtt.{MqttConnector, MqttDispatchingListener, MqttListenerMessage}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Filter
import play.api.routing.Router
import play.filters.csrf.CSRFComponents
import router.Routes
import scalikejdbc.config.DBs

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

trait EnvironmentSettingsConfig extends BuiltInComponents {
  lazy val config = HomeControllerConfiguration(
    mqttBrokerUrl = configuration.getString("home_center.mqtt.url").get,
    mqttClientId = configuration.getString("home_center.mqtt.clientId").get
  )
}

trait ClockConfig extends BuiltInComponents {
  lazy val clock = Clock.systemUTC()
}

trait SqlH2Config extends BuiltInComponents with EvolutionsComponents with DBComponents with HikariCPComponents  {
  lazy val initDb = {
    applicationEvolutions
    DBs.setupAll()
  }
}

trait DaoConfig extends BuiltInComponents with ClockConfig {
  lazy val wateringDao = wire[WateringDao]
  lazy val bcMeasureDao = wire[BcMeasureDao]
  def initDbAggregation():Unit = {
    actorSystem.scheduler.schedule(
      10 second,
      1 hour,
      new Runnable {
        override def run() = {
          bcMeasureDao.sensorAggregation()
          wateringDao.sensorAggregation()
        }
      }
    )
  }
}

trait MqttConfig extends BuiltInComponents
  with DaoConfig
  with EnvironmentSettingsConfig
  with ClockConfig {
  lazy val mqttDispatchingListener = wire[MqttDispatchingListener]
  lazy val mqttConnector = wire[MqttConnector]
  lazy val wateringCommander = wire[WateringCommander]

  lazy val wateringListener:ActorRef = actorSystem.actorOf(Props(wire[WateringListener]))
  lazy val WateringHelloListener:ActorRef = actorSystem.actorOf(Props(wire[WateringHelloListener]))
  lazy val bcBridgeListenerActor:ActorRef = actorSystem.actorOf(Props(wire[BridgeListener]))

  def initializeListeners():Unit = {
    bcBridgeListenerActor ! MqttListenerMessage.Ping
    wateringListener ! MqttListenerMessage.Ping
    WateringHelloListener ! MqttListenerMessage.Ping

    mqttDispatchingListener.addListener(bcBridgeListenerActor.path)
    mqttDispatchingListener.addListener(wateringListener.path)
    mqttDispatchingListener.addListener(WateringHelloListener.path)
  }
}

trait FiltersConfig extends BuiltInComponents with CSRFComponents {
  lazy val statsFilter: Filter = wire[StatsCounterFilter]
  lazy override val httpFilters = Seq(statsFilter, csrfFilter)

  lazy val statsActor = actorSystem.actorOf(Props(wire[StatsActor]), StatsActor.name)
}

trait Controllers extends BuiltInComponents with SqlH2Config with SilhouetteAppModule with MqttConfig {
  lazy val homeController = wire[HomeController]
  lazy val wateringController = wire[WateringController]
  lazy val bigClownController = wire[BigClownController]
  lazy val signinController: SignInController = wire[SignInController]
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
  statsActor ! Ping
  initDb
  initDbAggregation()
  initializeListeners()

  applicationLifecycle.addStopHook(() => {
    mqttConnector.disconnect().map(_ =>
      Future {
        DBs.closeAll()
      }
    )
  })
}

