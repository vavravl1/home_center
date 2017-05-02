package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.{BcMeasureDao, TimeGranularity}
import model.{AggregatedValues, SensorRepository}
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
                          sensorRepository: SensorRepository, //TODO remove me
                          silhouette: Silhouette[DefaultEnv]) extends Controller {
  def getSensorReading(location: String, position: String, name: String, timeGranularity: String, big: String) = Action.async {
    Future {
      val data:Seq[AggregatedValues] =
        sensorRepository
        .find(location + "/" + position, name)
        .map(sensor => sensor.getAggregatedValues(TimeGranularity.parse(timeGranularity, big.toBoolean)))
        .getOrElse(Seq.empty)
      Ok(Json.toJson(data))
    }
  }

  def getAvailableBcSensors = Action.async {
    Future {
      Ok(Json.toJson(
        sensorRepository.findAll()
      ))
    }
  }

  def cleanSensor(location: String, position: String, sensor: String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        bcMeasureDao.cleanSensor(location + "/" + position, sensor)
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
