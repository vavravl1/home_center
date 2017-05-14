package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.TimeGranularity
import model.{Measurement, SensorRepository}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Controller for the BigClown sensors
  */
class BigClownController(
                          sensorRepository: SensorRepository,
                          silhouette: Silhouette[DefaultEnv]) extends Controller {
  def getSensorReading(locationAddress: String, sensorName: String, timeGranularity: String, big: String): Action[AnyContent] = Action.async {
    Future {
      val data:Seq[Measurement] =
        sensorRepository
        .find(locationAddress, sensorName)
        .map(sensor => sensor.measuredPhenomenons.head)
        .map(measuredPhenomenon => measuredPhenomenon.measurements(TimeGranularity.parse(timeGranularity, big.toBoolean)))
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

  def cleanSensor(locationAddress: String, sensorName: String) = silhouette.SecuredAction.async { implicit request =>
    Future {
      if (request.identity.admin) {
        sensorRepository
          .find(locationAddress, sensorName)
          .foreach(sensor => sensorRepository.delete(sensor))
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
