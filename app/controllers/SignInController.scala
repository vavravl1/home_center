package controllers

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import security.forms.SignInForm
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import security.utils.auth.DefaultEnv
import java.time.Clock

import dao.HomeCenterUsersDao
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * The `Sign In` controller.
 *
 * @param silhouette The Silhouette stack.
 * @param usersDao Dao for retrieving users
 * @param authInfoRepository The auth info repository implementation.
 * @param credentialsProvider The credentials provider.
 * @param configuration The Play configuration.
 * @param clock The clock instance.
 */
class SignInController(
                        silhouette: Silhouette[DefaultEnv],
                        usersDao: HomeCenterUsersDao,
                        authInfoRepository: AuthInfoRepository,
                        credentialsProvider: CredentialsProvider,
                        configuration: Configuration,
                        clock: Clock
                      )
  extends Controller {

  /**
   * Views the `Sign In` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.index(None, CSRFHelper.token())))
  }


  /**
    * Manages the sign out action.
    */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Redirect(routes.HomeController.indexReact()))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.index(None, CSRFHelper.token()))),
      data => {
        val credentials = Credentials(data.email, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(routes.HomeController.indexReact())
          usersDao.retrieve(loginInfo).flatMap {
            case Some(user) =>
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator if data.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = new DateTime(clock.instant().toEpochMilli) + (7 days)
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, result)
                }
              }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case e: ProviderException =>
            Unauthorized(views.html.index(None,  CSRFHelper.token(), Some("signIn/?error=invalid.credentials")))
        }
      }
    )
  }
}
