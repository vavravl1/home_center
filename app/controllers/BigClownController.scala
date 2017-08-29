package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.TimeGranularity
import model.location.LocationRepository
import model.sensor.{MeasuredPhenomenon, SensorRepository}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import security.utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Controller for the BigClown sensors
  */
class BigClownController(
                          locationRepository: LocationRepository,
                          sensorRepository: SensorRepository,
                          silhouette: Silhouette[DefaultEnv]) extends Controller {
  def getSensorReading(locationAddress: String, sensorName: String, timeGranularity: String, big: String): Action[AnyContent] = Action.async {
    Future {
      val data:Seq[MeasuredPhenomenon] = sensorRepository
          .find(locationRepository.findOrCreateLocation(locationAddress), sensorName)
          .map(sensor => sensor.measuredPhenomenons)
          .getOrElse(Seq.empty)
      Ok(Json.toJson(data)
        (MeasuredPhenomenon.writesWithMeasurements(TimeGranularity.parse(timeGranularity, big.toBoolean)))
      )
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
          .find(locationRepository.findOrCreateLocation(locationAddress), sensorName)
          .foreach(sensor => sensorRepository.delete(sensor))
        NoContent
      } else {
        Unauthorized
      }
    }
  }
}
