package loader

import java.time.Duration

import akka.actor.Props
import com.softwaremill.macwire.wire
import model.actuator.impl.ActuatorRepositoryNaive
import model.actuator.{Command, CommandArgument}
import model.ifthen.{AverageValueChanged, DelayedCondition, IfThen, MqttIfThenExecutor}
import model.sensor.IdentityMeasurementAggregationStrategy
import play.api.BuiltInComponents

/**
  * All if-then related logic
  */
trait IfThenConfig extends BuiltInComponents with DaoConfig with ClockConfig with MqttConfig {

  lazy val actuatorRepository: ActuatorRepositoryNaive = wire[ActuatorRepositoryNaive]

  private lazy val livingRoomLocation = locationRepository.findOrCreateLocation("836d19839558")
  private lazy val pushButtonSensor = sensorRepository.findOrCreateSensor(livingRoomLocation, "push-button")
  private lazy val lightRelayLocation = locationRepository.findOrCreateLocation("836d19833c33")

  private lazy val displayLocation = locationRepository.findOrCreateLocation("836d19822676")
  private lazy val mainSwitchboardLocation = locationRepository.findOrCreateLocation("main-switchboard")
  private lazy val wattrouterSensor = sensorRepository.findOrCreateSensor(mainSwitchboardLocation, "wattrouter")

  private lazy val livingRoomTemperature = sensorRepository.findOrCreateSensor(livingRoomLocation, "thermometer")

  private lazy val terraceLocation = locationRepository.findOrCreateLocation("836d1982282c")
  private lazy val terraceTemperature = sensorRepository.findOrCreateSensor(terraceLocation, "thermometer")

  private lazy val upstairsLocation = locationRepository.findOrCreateLocation("836d19833c33")
  private lazy val co2Upstairs = sensorRepository.findOrCreateSensor(upstairsLocation, "co2-meter")

  lazy val mqttIfThenExecutor = actorSystem.actorOf(Props(new MqttIfThenExecutor(
    mqttBigClownParser,
    Seq(
      new IfThen(
        objekt = pushButtonSensor,
        subject = pushButtonSensor.findOrCreatePhenomenon(
          "event-count", "event-count", IdentityMeasurementAggregationStrategy),
        condition = AverageValueChanged,
        actuatorRepository.findOrCreateActuator(lightRelayLocation, "Relay"),
        Command("Toggle", Seq.empty)
      )
      ,
      new IfThen(
        objekt = wattrouterSensor,
        subject = wattrouterSensor.findOrCreatePhenomenon(
          "L1", "kW", IdentityMeasurementAggregationStrategy),
        condition = DelayedCondition(clock, Duration.ofSeconds(5)),
        actuatorRepository.findOrCreateActuator(displayLocation, "Display"),
        Command("Update", Seq(CommandArgument("power", "kW", "?")))
      ),
      new IfThen(
        objekt = livingRoomTemperature,
        subject = livingRoomTemperature.findOrCreatePhenomenon(
          "temperature", "\u2103", IdentityMeasurementAggregationStrategy),
        condition = DelayedCondition(clock, Duration.ofSeconds(5)),
        actuatorRepository.findOrCreateActuator(displayLocation, "Display"),
        Command("Update", Seq(CommandArgument("living-room", "\u2103", "?")))
      ),
      new IfThen(
        objekt = terraceTemperature,
        subject = terraceTemperature.findOrCreatePhenomenon(
          "temperature", "\u2103", IdentityMeasurementAggregationStrategy),
        condition = DelayedCondition(clock, Duration.ofSeconds(5)),
        actuatorRepository.findOrCreateActuator(displayLocation, "Display"),
        Command("Update", Seq(CommandArgument("terrace", "\u2103", "?")))
      ),
      new IfThen(
        objekt = co2Upstairs,
        subject = co2Upstairs.findOrCreatePhenomenon(
          "concentration", "ppm", IdentityMeasurementAggregationStrategy),
        condition = DelayedCondition(clock, Duration.ofSeconds(5)),
        actuatorRepository.findOrCreateActuator(displayLocation, "Display"),
        Command("Update", Seq(CommandArgument("co2", "ppm", "?")))
      )
    )
  )))

  def initialize(): Unit = {
    actuatorRepository.initialize
    mqttDispatchingListener.addListener(mqttIfThenExecutor.path)
  }
}

