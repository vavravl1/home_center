package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.{BcMeasureDao, TimeGranularity}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Controller for the BigClown sensors
  */
class BigClownController(
                          bcMeasureDao: BcMeasureDao,
                          silhouette: Silhouette[DefaultEnv]) extends Controller {
  def getSensorReading(location: String, position: String, sensor: String, timeGranularity: String, big: String) = Action.async {
    Future {
      Ok(Json.toJson(
        bcMeasureDao.getSampledMeasures(
          location + "/" + position,
          sensor,
          TimeGranularity.parse(timeGranularity, big.toBoolean)
        )))
    }
  }

  def getAvailableBcSensors = Action.async {
    Future {
      Ok(Json.toJson(
        bcMeasureDao.getAvailableBcSensors()
      ))
    }
  }

  def cleanSensor(location: String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        bcMeasureDao.cleanSensor(location)
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
