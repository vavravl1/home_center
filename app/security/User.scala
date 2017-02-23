package security

import com.mohiva.play.silhouette.api.Identity
import com.mohiva.play.silhouette.api.util.PasswordInfo

/**
 * The user object.
 *
 * @param email mail of the authenticated provider.
 * @param admin is true when the user can operate home_center, false otherwise
 * @param password hashed password of the user
 */
case class User(
  email: String,
  admin: Boolean,
  password: PasswordInfo) extends Identity

