package loader

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticator, CookieAuthenticatorService, CookieAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.softwaremill.macwire._
import dao.HomeCenterUsersDao
import play.api.BuiltInComponents
import security.utils.auth.{CustomSecuredErrorHandler, CustomUnsecuredErrorHandler, DefaultEnv}

import scala.concurrent.duration._
import scala.language.postfixOps

trait SilhouetteAppModule extends BuiltInComponents{

  import play.api.libs.concurrent.Execution.Implicits._

  lazy val fingerprintGenerator = new DefaultFingerprintGenerator(false)
  lazy val idGenerator = new SecureRandomIDGenerator()

  lazy val authenticatorService: AuthenticatorService[CookieAuthenticator] =
    new CookieAuthenticatorService(
      CookieAuthenticatorSettings(
        cookieName = "id",
        cookiePath = "/",
        cookieDomain = None,
        secureCookie = false,
        httpOnlyCookie = true,
        encryptAuthenticator = false,
        useFingerprinting = true,
        cookieMaxAge = Some(12 hours),
        authenticatorIdleTimeout = Some(30 minutes),
        authenticatorExpiry = 12 hours),
      None,
      fingerprintGenerator,
      idGenerator,
      Clock()
    )

  lazy val eventBus = EventBus()

  lazy val passwordHasher = new BCryptPasswordHasher()
  lazy val homeCenterUsersDao:HomeCenterUsersDao = wire[HomeCenterUsersDao]

  private lazy val env: Environment[DefaultEnv] = Environment[DefaultEnv](
    homeCenterUsersDao, authenticatorService, List(), eventBus
  )

  lazy val securedErrorHandler: SecuredErrorHandler = wire[CustomSecuredErrorHandler]
  lazy val unSecuredErrorHandler: UnsecuredErrorHandler = wire[CustomUnsecuredErrorHandler]

  lazy val securedAction: SecuredAction = new DefaultSecuredAction(new DefaultSecuredRequestHandler(securedErrorHandler))
  lazy val unsecuredAction: UnsecuredAction = new DefaultUnsecuredAction(new DefaultUnsecuredRequestHandler(unSecuredErrorHandler))

  lazy val userAwareAction = new DefaultUserAwareAction(new DefaultUserAwareRequestHandler)

  lazy val authInfoRepository = new DelegableAuthInfoRepository(homeCenterUsersDao)

  lazy val credentialsProvider = new CredentialsProvider(authInfoRepository, passwordHasher, List(passwordHasher))

  lazy val silhouette: Silhouette[DefaultEnv] = wire[SilhouetteProvider[DefaultEnv]]
}
