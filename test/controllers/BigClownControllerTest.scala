package controllers

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

import entities.bigclown.{AggregatedBcMeasure, BcMeasure, Location}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
  *
  */
class BigClownControllerTest extends WordSpec with Matchers with IntegrationTest {

  "BigClownController" when {
    val token = getCsrfToken()
    val now = Instant.now()
    appComponents.bcMeasureDao.cleanDb()

    "when there are old measures" should {
      appComponents.locationDao.saveOrUpdate(Location("remote/0", "location-label"))
      appComponents.bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", now, 10, "C"))
      appComponents.bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", now.minus(70, MINUTES), 20, "C"))
      appComponents.bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", now.minus(119, MINUTES), 30, "C"))
      appComponents.bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", now.minus(130, MINUTES), 50, "C")) // Not there

      "it returns correct values for big scale" in {
        val request = FakeRequest(GET, "/bc/remote/0/temperature?timeGranularity=ByMinute&big=true")
        val bcMeasures = route(app, request).get

        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]
        contentAsJson(bcMeasures).as[JsArray].value.size shouldBe 3
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(0))(AggregatedBcMeasure.format)).get.average shouldBe 30.0
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(1))(AggregatedBcMeasure.format)).get.average shouldBe 20.0
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(2))(AggregatedBcMeasure.format)).get.average shouldBe 10.0
      }

      "it returns correct values for small scale" in {
        val request = FakeRequest(GET, "/bc/remote/0/temperature?timeGranularity=ByMinute&big=false")
        val bcMeasures = route(app, request).get

        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]
        contentAsJson(bcMeasures).as[JsArray].value.size shouldBe 1
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(0))(AggregatedBcMeasure.format)).get.average shouldBe 10.0
      }
    }
  }
}
