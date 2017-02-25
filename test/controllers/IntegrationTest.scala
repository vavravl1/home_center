package controllers

import java.io.File

import loader.{AppApplicationLoader, AppComponents}
import mqtt.MqttConnector
import play.api.ApplicationLoader.Context
import play.api._
import play.core.{DefaultWebCommands, SourceMapper, WebCommands}

/**
  *
  */
trait IntegrationTest {

  trait WithoutMqttAppComponents extends AppComponents {
    override lazy val mqttConnector = new MqttConnector(null, null, actorSystem) {
      override def reconnect() = new Runnable {
        override def run() = {}
      }
    }
  }

  class WithoutMqttApp extends AppApplicationLoader {
    override def createApp(context: Context) =
      new BuiltInComponentsFromContext(context) with WithoutMqttAppComponents
  }

  val app = new WithoutMqttApp()
    .load(ApplicationLoader.Context(environment, sourceMapper, webCommands, configuration))

  def sourceMapper: Option[SourceMapper] = None
  def environment: Environment = Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test)
  def webCommands: WebCommands = new DefaultWebCommands
  def configuration: Configuration = Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test))
}
