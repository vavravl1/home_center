package controllers

import play.api.mvc._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.Token

/**
  * Helper object for handling CSRF tokens
  */
object CSRFHelper {
  def token()(implicit request: RequestHeader): Token =
    CSRF.getToken(request) getOrElse Token("csrf","not there")
}
