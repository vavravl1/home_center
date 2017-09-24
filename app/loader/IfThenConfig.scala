package loader

import akka.actor.Props
import com.softwaremill.macwire.wire
import model.actuator.Command
import model.actuator.impl.ActuatorRepositoryNaive
import model.ifthen.{AverageValueChanged, IfThen, MqttIfThenExecutor}
import model.sensor.IdentityMeasurementAggregationStrategy
import play.api.BuiltInComponents

/**
  * All if-then related logic
  */
trait IfThenConfig extends BuiltInComponents with DaoConfig with ClockConfig with MqttConfig {

  lazy val actuatorRepository: ActuatorRepositoryNaive = wire[ActuatorRepositoryNaive]

  private lazy val location = locationRepository.findOrCreateLocation("836d19839558")
  private lazy val sensor = sensorRepository.findOrCreateSensor(location, "push-button")
  private lazy val actuatorLocation = locationRepository.findOrCreateLocation("836d19833c33")
  lazy val mqttIfThenExecutor = actorSystem.actorOf(Props(new MqttIfThenExecutor(
      mqttBigClownParser,
      Seq(
        new IfThen(
          objekt = sensor,
          subject = sensor.findOrCreatePhenomenon(
            "event-count", "event-count", IdentityMeasurementAggregationStrategy),
          condition = AverageValueChanged,
          actuatorRepository.findOrCreateActuator(actuatorLocation, "Relay"),
          Command("Toggle", Seq.empty)
      )
    )
  )))

  def initializeIfThens(): Unit = {
    mqttDispatchingListener.addListener(mqttIfThenExecutor.path)
  }
}

