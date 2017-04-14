package controllers

import java.io.File

import loader.{AppApplicationLoader, AppComponents}
import mqtt.MqttConnector
import play.api.test.Helpers._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route}
import play.core.{DefaultWebCommands, SourceMapper, WebCommands}
import play.filters.csrf.CSRF.Token
/**
  *
  */
trait IntegrationTest {

  var appComponents:BuiltInComponentsFromContext with WithoutMqttAppComponents = null

  trait WithoutMqttAppComponents extends AppComponents {
    override lazy val mqttConnector = new MqttConnector(null, null, actorSystem) {
      override val reconnect = new Runnable {
        override def run() = {}
      }
    }
  }

  class WithoutMqttApp extends AppApplicationLoader {
    override def createApp(context: Context) = {
      appComponents = new BuiltInComponentsFromContext(context) with WithoutMqttAppComponents
      appComponents
    }
  }

  implicit val app = new WithoutMqttApp()
    .load(ApplicationLoader.Context(environment, sourceMapper, webCommands, configuration))

  Play.start(app)

  def getCsrfToken() = {
    val request = FakeRequest(GET, "/data").withHeaders()
    val page = route(app, request).get
    val content = contentAsString(page)
    val name = ("""<input type="hidden" id="csrf_token_name" value="([\w]+)">""".r).findFirstMatchIn(content).get.group(1)
    val value = ("""<input type="hidden" id="csrf_token_value" value="([\w-]+)">""".r).findFirstMatchIn(content).get.group(1)
    Token(name, value)
  }

  def sourceMapper: Option[SourceMapper] = None
  def environment: Environment = Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test)
  def webCommands: WebCommands = new DefaultWebCommands
  def configuration: Configuration = Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test))
}
