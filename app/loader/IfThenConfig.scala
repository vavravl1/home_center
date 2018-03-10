package loader

import akka.actor.Props
import model.actuator.impl.ActuatorRepositoryNaive
import model.actuator.{ActuatorRepository, Command, CommandArgument}
import model.ifthen._
import model.sensor.DoubleValuesMeasurementAggregationStrategy
import mqtt.JsonSender
import play.api.BuiltInComponents

/**
  * All if-then related logic
  */
trait IfThenConfig extends BuiltInComponents with DaoConfig with ClockConfig {

  lazy val ibiscusLocation = locationRepository.findOrCreateLocation("836d1983a689")
  lazy val thermometerLivingRoom = sensorRepository.findOrCreateSensor(ibiscusLocation, "thermometer")

  lazy val lightRelayLocation = locationRepository.findOrCreateLocation("836d19833c33")

  lazy val blindsLocation = locationRepository.findOrCreateLocation("836d19822676")
  lazy val blindsPushButtonSensor = sensorRepository.findOrCreateSensor(blindsLocation, "push-button")
  lazy val blindsButton = blindsPushButtonSensor.findOrCreatePhenomenon(
    "event-count", "event-count", DoubleValuesMeasurementAggregationStrategy)
  lazy val blindsBlinds = sensorRepository.findOrCreateSensor(blindsLocation, "generic")
  lazy val blindsBlindsMove = blindsBlinds.findOrCreatePhenomenon("move", "move", DoubleValuesMeasurementAggregationStrategy)

  lazy val mainSwitchboardLocation = locationRepository.findOrCreateLocation("main-switchboard")
  lazy val wattrouterSensor = sensorRepository.findOrCreateSensor(mainSwitchboardLocation, "wattrouter")

  lazy val terraceLocation = locationRepository.findOrCreateLocation("836d1982282c")
  lazy val terraceTemperature = sensorRepository.findOrCreateSensor(terraceLocation, "thermometer")
  lazy val terraceButton = sensorRepository.findOrCreateSensor(terraceLocation, "push-button")
  lazy val terraceLuxmeter = sensorRepository.findOrCreateSensor(terraceLocation, "lux-meter")
  lazy val terraceIlluminance = terraceLuxmeter.findOrCreatePhenomenon("illuminance", unit = "lux", aggregationStrategy = DoubleValuesMeasurementAggregationStrategy)

  lazy val bedroomLocation = locationRepository.findOrCreateLocation("836d19839558")
  lazy val co2Bedroom = sensorRepository.findOrCreateSensor(bedroomLocation, "co2-meter")
  lazy val thermometerBedroom = sensorRepository.findOrCreateSensor(bedroomLocation, "thermometer")

  lazy val garageLocation = locationRepository.findOrCreateLocation("garage")
  lazy val fve = sensorRepository.findOrCreateSensor(garageLocation, "pve-inverter")

  def prepareActuatorRepository(jsonSender: JsonSender) = new ActuatorRepositoryNaive(
    locationRepository = locationRepository,
    sensorRepository = sensorRepository,
    jsonSender = jsonSender
  )

  def prepareIfThenExecutor(
                             actuatorRepository: ActuatorRepository
                           ) = actorSystem.actorOf(Props(
    new SensorMeasurementsIfThenExecutor(
      Seq(
        new IfThen(
          objekt = terraceButton,
          subject = terraceButton.findOrCreatePhenomenon(
            "event-count", "event-count", DoubleValuesMeasurementAggregationStrategy),
          condition = AverageValueChanged,
          actuatorRepository.findOrCreateActuator(lightRelayLocation, "Relay"),
          () => Command("Toggle", Seq.empty)
        ),
        new IfThen(
          objekt = blindsPushButtonSensor,
          subject = blindsButton,
          condition = TrueCondition,
          actuatorRepository.findOrCreateActuator(blindsLocation, "Display"),
          () => Command("Update", Seq(
            CommandArgument("power", "kW", String.valueOf(
              wattrouterSensor
                .findOrCreatePhenomenon("L1", "kW", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            )),
            CommandArgument("fve", "W", String.valueOf(
              fve
                .findOrCreatePhenomenon("power", "W", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            )),
            CommandArgument("living-room", "\u2103", String.valueOf(
              thermometerLivingRoom
                .findOrCreatePhenomenon("temperature", "\u2103", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            )),
            CommandArgument("terrace", "\u2103", String.valueOf(
              terraceTemperature
                .findOrCreatePhenomenon("temperature", "\u2103", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            )),
            CommandArgument("bedroom", "\u2103", String.valueOf(
              thermometerBedroom
                .findOrCreatePhenomenon("temperature", "\u2103", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            )),
            CommandArgument("co2", "ppm", String.valueOf(
              co2Bedroom
                .findOrCreatePhenomenon("concentration", "ppm", DoubleValuesMeasurementAggregationStrategy)
                .lastNMeasurementsDescendant(1).map(_.average).headOption.getOrElse(0)
            ))
          ))
        ),
        new IfThen(
          objekt = terraceLuxmeter,
          subject = terraceIlluminance,
          condition = AndCondition(
            GreaterThan(300),
            NoMeasurementsInLast4Hours(
              blindsBlindsMove,
              clock
            )
          ),
          actuator = actuatorRepository.findOrCreateActuator(blindsLocation, "Blinds"),
          command = () => Command("Up", Seq.empty)
        ),
        new IfThen(
          objekt = terraceLuxmeter,
          subject = terraceIlluminance,
          condition = AndCondition(
            LowerThan(80),
            NoMeasurementsInLast4Hours(
              blindsBlindsMove,
              clock
            )
          ),
          actuator = actuatorRepository.findOrCreateActuator(blindsLocation, "Blinds"),
          command = () => Command("Down", Seq.empty)
        )
      ))))
}

