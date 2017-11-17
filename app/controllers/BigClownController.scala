package controllers

import com.mohiva.play.silhouette.api.Silhouette
import dao.TimeGranularity
import model.location.LocationRepository
import model.sensor.{MeasuredPhenomenon, Measurement, SensorRepository}
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, AnyContent, Controller, Result}
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
    val measuredPhenomenons: Seq[MeasuredPhenomenon] = sensorRepository
      .find(locationRepository.findOrCreateLocation(locationAddress), sensorName)
      .map(sensor => sensor.measuredPhenomenons)
      .getOrElse(Seq.empty)

    val measurements: Seq[Future[JsObject]] = measuredPhenomenons
      .map(measuredPhenomenon => {
        measuredPhenomenon.measurements(TimeGranularity.parse(timeGranularity, big.toBoolean))
          .map((measurements: Seq[Measurement]) => Json.obj(
            "name" -> measuredPhenomenon.name,
            "unit" -> measuredPhenomenon.unit,
            "measurements" -> Json.toJson(measurements),
            "aggregationStrategy" -> Json.toJson(measuredPhenomenon.aggregationStrategy)
          ))
      })

    val result: Future[Result] = Future.sequence(measurements)
      .map((jsons: Seq[JsObject]) => Ok(JsArray(jsons)))
    result
  }

  def getAvailableBcSensors: Action[AnyContent] = Action.async {
    Future {
      Ok(Json.toJson(
        sensorRepository.findAll()
      ))
    }
  }

  def cleanSensor(locationAddress: String, sensorName: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
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
