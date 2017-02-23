package dao

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.Configuration
import security.User

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Dao for retrieving users for home center application
  */
class HomeCenterUsersDao(passwordHasher: BCryptPasswordHasher, configuration: Configuration) extends DelegableAuthInfoDAO[PasswordInfo] with IdentityService[User]  {

  private var data:mutable.Map[LoginInfo, User] = mutable.Map()
  loadFromConfig().foreach(user => data.put(LoginInfo("credentials", user.email), user))

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = Future {
    data.get(loginInfo).map(u => u.password)
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = Future {
    data.put(loginInfo, User(null, false, authInfo))
    authInfo
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = Future {
    val oldU = data.get(loginInfo).get
    data.remove(loginInfo)
    val newU = User(oldU.email, oldU.admin, authInfo)
    data.put(loginInfo, newU)
    authInfo
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = Future {
    if(data.contains(loginInfo)) {
      update(loginInfo, authInfo)
    } else {
      add(loginInfo, authInfo)
    }
    authInfo
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future {
    data.remove(loginInfo)
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future {
    data.get(loginInfo)
  }

  def loadFromConfig():Seq[User] = {
    configuration.getConfigSeq("home_center.users").map(confs => confs.map(userConf =>
      User(
        userConf.getString("email").get,
        userConf.getBoolean("admin").get,
        passwordHasher.hash(userConf.getString("password").get)
      )
    )).get
  }
}
