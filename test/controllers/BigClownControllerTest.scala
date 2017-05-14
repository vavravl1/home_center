package controllers

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

import model.{AggregatedValue, Measurement}
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

    appComponents.sensorRepository.findAll()
      .foreach(s => appComponents.sensorRepository.delete(s))

    val location = appComponents.locationRepository.findOrCreateLocation("remote-0")
    location.updateLabel("location-label")

    val sensor = appComponents.sensorRepository.findOrCreateSensor("remote-0", "thermometer")

    "when there are old measures" should {
      sensor.addMeasurement(Measurement(10, now), "temperature", "C")
      sensor.addMeasurement(Measurement(20, now.minus(70, MINUTES)), "temperature", "C")
      sensor.addMeasurement(Measurement(30, now.minus(119, MINUTES)), "temperature", "C")
      sensor.addMeasurement(Measurement(50, now.minus(130, MINUTES)), "temperature", "C")

      "it returns correct values for big scale" in {
        val request = FakeRequest(GET, "/bc/remote-0/thermometer?timeGranularity=ByMinute&big=true")
        val bcMeasures = route(app, request).get

        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]
        contentAsJson(bcMeasures).as[JsArray].value.size shouldBe 3
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(0))(AggregatedValue.format)).get.average shouldBe 30.0
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(1))(AggregatedValue.format)).get.average shouldBe 20.0
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(2))(AggregatedValue.format)).get.average shouldBe 10.0
      }

      "it returns correct values for small scale" in {
        val request = FakeRequest(GET, "/bc/remote-0/thermometer?timeGranularity=ByMinute&big=false")
        val bcMeasures = route(app, request).get

        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]
        contentAsJson(bcMeasures).as[JsArray].value.size shouldBe 1
        (Json.fromJson(contentAsJson(bcMeasures).as[JsArray].value(0))(AggregatedValue.format)).get.average shouldBe 10.0
      }
    }
  }
}
