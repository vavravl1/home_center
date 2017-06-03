package controllers

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES

import model.{Measurement, NoneMeasurementAggregationStrategy}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, JsObject, Json}
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

    val sensor = appComponents.sensorRepository.findOrCreateSensor(location, "thermometer")
    val phenomenon = sensor.findOrCreatePhenomenon("temperature", "C", NoneMeasurementAggregationStrategy)

    "when there are old measures" should {
      sensor.addMeasurement(Measurement(10, now), phenomenon)
      sensor.addMeasurement(Measurement(20, now.minus(70, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(30, now.minus(119, MINUTES)), phenomenon)
      sensor.addMeasurement(Measurement(50, now.minus(130, MINUTES)), phenomenon)

      "return correct values for big scale" in {
        val request = FakeRequest(GET, "/bc/remote-0/thermometer?timeGranularity=ByMinute&big=true")
        val bcMeasures = route(app, request).get

        val measurementsArray = contentAsJson(bcMeasures).as[JsArray].value.head
          .as[JsObject].value("measurements").as[JsArray]
        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]
        measurementsArray.value.size shouldBe 3
        (Json.fromJson(measurementsArray.value(0))(Measurement.format)).get.average shouldBe 30.0
        (Json.fromJson(measurementsArray.value(1))(Measurement.format)).get.average shouldBe 20.0
        (Json.fromJson(measurementsArray.value(2))(Measurement.format)).get.average shouldBe 10.0
      }

      "return correct values for small scale" in {
        val request = FakeRequest(GET, "/bc/remote-0/thermometer?timeGranularity=ByMinute&big=false")
        val bcMeasures = route(app, request).get

        status(bcMeasures) shouldBe OK
        contentAsJson(bcMeasures) shouldBe a [JsArray]

        val measurementsArray = contentAsJson(bcMeasures).as[JsArray].value.head
          .as[JsObject].value("measurements").as[JsArray]
        measurementsArray.value.size shouldBe 1
        (Json.fromJson(measurementsArray.value(0))(Measurement.format)).get.average shouldBe 10.0
      }
    }
  }
}
