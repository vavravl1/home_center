package loader

import com.softwaremill.macwire.wire
import controllers._
import play.api.BuiltInComponents

/**
  * All controllers
  */
trait Controllers extends BuiltInComponents with SilhouetteAppModule with MqttConfig with WsClientConfig with IfThenConfig{
  lazy val homeController = wire[HomeController]
  lazy val bigClownController = wire[BigClownController]
  lazy val signinController: SignInController = wire[SignInController]
  lazy val settingsController: SettingsController = wire[SettingsController]
  lazy val actuatorController: ActuatorController = wire[ActuatorController]
}

