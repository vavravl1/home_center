package loader

import akka.actor.Props
import model.actuator.impl.ActuatorRepositoryNaive
import model.actuator.{ActuatorRepository, Command, CommandArgument}
import model.ifthen._
import model.sensor.IdentityMeasurementAggregationStrategy
import mqtt.JsonSender
import play.api.BuiltInComponents

/**
  * All if-then related logic
  */
trait IfThenConfig extends BuiltInComponents with DaoConfig with ClockConfig {

  lazy val ibiscusLocation = locationRepository.findOrCreateLocation("836d1983a689")
  lazy val livingRoomTemperature = sensorRepository.findOrCreateSensor(ibiscusLocation, "thermometer")

  lazy val lightRelayLocation = locationRepository.findOrCreateLocation("836d19833c33")

  lazy val displayLocation = locationRepository.findOrCreateLocation("836d19822676")
  lazy val displayButton = sensorRepository.findOrCreateSensor(displayLocation, "push-button")

  lazy val mainSwitchboardLocation = locationRepository.findOrCreateLocation("main-switchboard")
  lazy val wattrouterSensor = sensorRepository.findOrCreateSensor(mainSwitchboardLocation, "wattrouter")

  lazy val terraceLocation = locationRepository.findOrCreateLocation("836d1982282c")
  lazy val terraceTemperature = sensorRepository.findOrCreateSensor(terraceLocation, "thermometer")
  lazy val terraceButton = sensorRepository.findOrCreateSensor(terraceLocation, "push-button")

  lazy val bedroomLocation = locationRepository.findOrCreateLocation("836d19839558")
  lazy val co2Bedroom = sensorRepository.findOrCreateSensor(bedroomLocation, "co2-meter")

  def prepareActuatorRepository(jsonSender: JsonSender) = {
    new ActuatorRepositoryNaive(
      locationRepository = locationRepository,
      sensorRepository = sensorRepository,
      jsonSender = jsonSender
    )
  }

  def prepareIfThenExecutor(actuatorRepository: ActuatorRepository) = {
    actorSystem.actorOf(Props(new SensorMeasurementsIfThenExecutor(
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
              co2Bedroom
                .findOrCreatePhenomenon("concentration", "ppm", IdentityMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            ))
          ))
        ))
    )))
  }
}

