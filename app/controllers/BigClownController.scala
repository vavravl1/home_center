package controllers

import dao.{BcMeasureDao, TimeGranularity}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * Controller for the BigClown sensors
  */
class BigClownController(bcMeasureDao: BcMeasureDao) extends Controller {
  def getSensorReading(sensor: String, timeGranularity: String) = Action.async {
    Future {
      Ok(Json.toJson(
        bcMeasureDao.getSampledMeasures(
          sensor,
          TimeGranularity.parse(timeGranularity)
        )))
    }
  }
}
