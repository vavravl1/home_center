package model.thermostat

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class ThermostatTest extends WordSpec with Matchers with MockFactory {
//  "Thermostat" when {
//    implicit val system = ActorSystem()
//
//    val temperatureSensor = stub[Sensor]
//    val temperaturePhenomenon = stub[MeasuredPhenomenonTemperature]
//    val activatorSensor = stub[Sensor]
//    val activatorPhenomenon = stub[MeasuredPhenomenon]
//    val actuator = stub[Actuator]
//
//    "given single evaluator" should {
//      val dateInsideEvaluator = LocalDateTime.of(2017, 1, 1, 5, 50, 0)
//      val actuatorActivator = new ActuatorActivator(
//        activatorSensor = activatorSensor,
//        activatorPhenomenon = activatorPhenomenon,
//        actuator = actuator
//      )
//      val thermostat: TestActorRef[Thermostat] = TestActorRef[Thermostat](Props(
//        new Thermostat(
//          temperatureSensors = Seq(temperatureSensor),
//          evaluators = Seq(SingleSensorThermostatEvaluator(
//            timeEvaluator = ThermostatTimeEvaluator(
//              startTime = LocalDateTime.of(2017, 1, 1, 5, 30, 0),
//              endTime = LocalDateTime.of(2017, 1, 1, 6, 30, 0)
//            ),
//            valueEvaluator = ThermostatValueEvaluator(20.0, 21.0)
//          )),
//          defaultCondition = ThermostatValueEvaluator(18.0, 18.5),
//          actuatorActivator = actuatorActivator
//        )
//      ))
//      "does not react on non temperature sensor" in {
//        val nonTemperatureSensor = mock[Sensor]
//        thermostat ! ConsumeMessage(
//          nonTemperatureSensor,
//          temperaturePhenomenon,
//          Measurement(10, dateInsideEvaluator.toInstant(ZoneOffset.UTC))
//        )
//        (actuator.execute _).verify(*).never
//      }
//      "does not react on non temperature phenomenon" in {
//        val nonTemperaturePhenomenon = mock[MeasuredPhenomenon]
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          nonTemperaturePhenomenon,
//          Measurement(10, dateInsideEvaluator.toInstant(ZoneOffset.UTC))
//        )
//        (actuator.execute _).verify(*).never
//      }
//      "start heating in cold" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(19.7, dateInsideEvaluator.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluator)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("On", Seq())
//      }
//      "stop heating in warm" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(21.1, dateInsideEvaluator.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluator)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("Off", Seq())
//      }
//      "keep heating untouched when not yet warm" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(20.5, dateInsideEvaluator.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluator)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("", Seq())
//      }
//    }
//    "given two evaluators" should {
//      val dateInsideEvaluatorA = LocalDateTime.of(2017, 1, 1, 5, 50, 0)
//      val dateInsideEvaluatorB = LocalDateTime.of(2017, 1, 1, 18, 20, 0)
//      val dateInsideEvaluatorC = LocalDateTime.of(2017, 1, 1, 15, 30, 0)
//      val actuatorActivator = new ActuatorActivator(
//        activatorSensor = activatorSensor,
//        activatorPhenomenon = activatorPhenomenon,
//        actuator = actuator
//      )
//      val thermostat: TestActorRef[Thermostat] = TestActorRef[Thermostat](Props(
//        new Thermostat(
//          temperatureSensors = Seq(temperatureSensor),
//          evaluators = Seq(
//            SingleSensorThermostatEvaluator(
//              timeEvaluator = ThermostatTimeEvaluator(
//                startTime = LocalDateTime.of(2017, 1, 1, 5, 30, 0),
//                endTime = LocalDateTime.of(2017, 1, 1, 6, 30, 0)
//              ),
//              valueEvaluator = ThermostatValueEvaluator(20.0, 21.0)
//            ),
//            SingleSensorThermostatEvaluator(
//              timeEvaluator = ThermostatTimeEvaluator(
//                startTime = LocalDateTime.of(2017, 1, 2, 17, 30, 0),
//                endTime = LocalDateTime.of(2017, 1, 2, 18, 30, 0)
//              ),
//              valueEvaluator = ThermostatValueEvaluator(22.0, 23.0)
//            )
//          ),
//          defaultCondition = ThermostatValueEvaluator(18.0, 18.5),
//          actuatorActivator = actuatorActivator
//        )
//      ))
//      "use first evaluator in correct time" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(19.7, dateInsideEvaluatorA.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluatorA)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("On", Seq())
//      }
//      "use second evaluator in correct time" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(21.9, dateInsideEvaluatorB.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluatorB)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("On", Seq())
//      }
//      "use default evaluator if none of time matches" in {
//        thermostat ! ConsumeMessage(
//          temperatureSensor,
//          temperaturePhenomenon,
//          Measurement(18.6, dateInsideEvaluatorC.toInstant(ZoneId.systemDefault().getRules.getOffset(dateInsideEvaluatorC)))
//        )
//        actuatorActivator.getActualCommand() shouldBe Command("Off", Seq())
//      }
//    }
//    "given two sensors" should {
//      "start heating if one is cold" in {
//      }
//      "stop heating both are warm" in {
//      }
//      "keep heating when one is cold and one is warm" in {
//      }
//    }
//  }
//
//  class MeasuredPhenomenonTemperature extends MeasuredPhenomenon {
//    override val name: String = "temperature"
//    override val unit: String = "C"
//    override val aggregationStrategy: MeasurementAggregationStrategy = IdentityMeasurementAggregationStrategy
//    override def measurements(timeGranularity: TimeGranularity): Seq[Measurement] = Seq()
//    override def lastNMeasurementsDescendant(n: Int): Seq[Measurement] = Seq()
//    override def aggregateOldMeasurements(): Unit = {}
//    override def addMeasurement(measurement: Measurement): Unit = {}
//  }
}


