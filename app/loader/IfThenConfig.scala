package loader

import akka.actor.Props
import com.softwaremill.macwire.wire
import model.actuator.impl.ActuatorRepositoryNaive
import model.actuator.{Command, CommandArgument}
import model.ifthen._
import model.sensor.IdentityMeasurementAggregationStrategy
import play.api.BuiltInComponents

/**
  * All if-then related logic
  */
trait IfThenConfig extends BuiltInComponents with DaoConfig with ClockConfig with MqttConfig {
  lazy val actuatorRepository: ActuatorRepositoryNaive = wire[ActuatorRepositoryNaive]

  private lazy val ibiscusLocation = locationRepository.findOrCreateLocation("836d1983a689")
  private lazy val livingRoomTemperature = sensorRepository.findOrCreateSensor(ibiscusLocation, "thermometer")

  private lazy val lightRelayLocation = locationRepository.findOrCreateLocation("836d19833c33")

  private lazy val displayLocation = locationRepository.findOrCreateLocation("836d19822676")
  private lazy val displayButton = sensorRepository.findOrCreateSensor(displayLocation, "push-button")

  private lazy val mainSwitchboardLocation = locationRepository.findOrCreateLocation("main-switchboard")
  private lazy val wattrouterSensor = sensorRepository.findOrCreateSensor(mainSwitchboardLocation, "wattrouter")

  private lazy val terraceLocation = locationRepository.findOrCreateLocation("836d1982282c")
  private lazy val terraceTemperature = sensorRepository.findOrCreateSensor(terraceLocation, "thermometer")
  private lazy val terraceButton = sensorRepository.findOrCreateSensor(terraceLocation, "push-button")

  private lazy val upstairsLocation = locationRepository.findOrCreateLocation("836d19833c33")
  private lazy val co2Upstairs = sensorRepository.findOrCreateSensor(upstairsLocation, "co2-meter")

  lazy val mqttIfThenExecutor = actorSystem.actorOf(Props(new MqttIfThenExecutor(
    Seq(
      new IfThen(
        objekt = terraceButton,
        subject = terraceButton.findOrCreatePhenomenon(
          "event-count", "event-count", IdentityMeasurementAggregationStrategy),
        condition = AverageValueChanged,
        actuatorRepository.findOrCreateActuator(lightRelayLocation, "Relay"),
        () => Command("Toggle", Seq.empty)
      ),
      new IfThen(
        objekt = displayButton,
        subject = displayButton.findOrCreatePhenomenon(
          "event-count", "event-count", IdentityMeasurementAggregationStrategy),
        condition = TrueCondition,
        actuatorRepository.findOrCreateActuator(displayLocation, "Display"),
        () => Command("Update", Seq(
          CommandArgument("power", "kW", String.valueOf(
            wattrouterSensor
              .findOrCreatePhenomenon("L1", "kW", IdentityMeasurementAggregationStrategy)
              .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
          )),
          CommandArgument("living-room", "\u2103", String.valueOf(
            livingRoomTemperature
              .findOrCreatePhenomenon("temperature", "\u2103", IdentityMeasurementAggregationStrategy)
              .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
          )),
          CommandArgument("terrace", "\u2103", String.valueOf(
            terraceTemperature
              .findOrCreatePhenomenon("temperature", "\u2103", IdentityMeasurementAggregationStrategy)
              .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
          )),
          CommandArgument("co2", "ppm", String.valueOf(
            co2Upstairs
              .findOrCreatePhenomenon("concentration", "ppm", IdentityMeasurementAggregationStrategy)
              .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
          ))
        ))
      ))
  )))

  def initialize(): Unit = {
    actuatorRepository.initialize
    mqttDispatchingListener.addListener(mqttIfThenExecutor.path)
  }
}

