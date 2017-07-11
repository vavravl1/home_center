package loader

import com.softwaremill.macwire._
import controllers._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.routing.Router
import play.filters.csrf.CSRFComponents
import router.Routes
import scalikejdbc.config.DBs

import scala.concurrent.Future
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

trait FiltersConfig extends BuiltInComponents with CSRFComponents {
  lazy override val httpFilters = Seq(csrfFilter)
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

