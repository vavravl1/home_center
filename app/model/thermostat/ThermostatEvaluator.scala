package model.thermostat

import java.time.{Instant, LocalDateTime, ZoneId}

import model.sensor.{Measurement, Sensor}
import play.api.Logger

case class ThermostatEvaluator(
                                evaluators: Seq[SingleSensorThermostatEvaluator],
                                defaultCondition: ThermostatValueEvaluator
                              ) {
  def determineAction: ThermostatAction = {
    var actions = evaluators
      .filter(_.timeMatch)
      .map(_.temperatureMatch())

    if (actions.isEmpty) actions = defaultEvaluationWithoutTime

    if (actions.forall(_ == TurnOff)) {
      TurnOff
    } else if (actions.contains(TurnOn)) {
      TurnOn
    } else {
      KeepAsIs
    }
  }

  private def defaultEvaluationWithoutTime = {
    evaluators
      .map(_.lastMeasurement)
      .map {
        case Some(measurement) => defaultCondition.temperatureMatch(measurement.average)
        case None => KeepAsIs
      }
  }
}

case class SingleSensorThermostatEvaluator(
                                            temperatureSensor: Sensor,
                                            timeEvaluator: ThermostatTimeEvaluator,
                                            valueEvaluator: ThermostatValueEvaluator
                                          ) {
  def lastMeasurement: Option[Measurement] = {
    temperatureSensor.findPhenomenon("temperature")
      .map(_.lastNMeasurementsDescendant(1))
      .filter(_.size == 1)
      .map(_.head)
  }

  def timeMatch: Boolean = lastMeasurement
    .map(_.measureTimestamp)
    .exists(timeEvaluator.timeMatch)

  def temperatureMatch() = lastMeasurement
    .map(x => valueEvaluator.temperatureMatch(x.average))
    .map(x => {
      if(x == TurnOn) Logger.info(s"Turning heating on by Sensor " +
        s"in ${temperatureSensor.location.label} " +
        s"given temperature ${lastMeasurement.get.average} measured in ${lastMeasurement.get.measureTimestamp} " +
        s"matching time is ${timeEvaluator.startTime} - ${timeEvaluator.endTime}")
      x
    })
    .getOrElse(KeepAsIs)
}

case class ThermostatValueEvaluator(lowerBoundary: Double,
                                    upperBoundary: Double) {
  def temperatureMatch(temperature: Double): ThermostatAction = {
    if (temperature < lowerBoundary) {
      TurnOn
    } else if (temperature > upperBoundary) {
      TurnOff
    } else {
      KeepAsIs
    }
  }
}

case class ThermostatTimeEvaluator(
                                    startTime: LocalDateTime,
                                    endTime: LocalDateTime) {
  def timeMatch(whenInstant: Instant): Boolean = {
    val when = LocalDateTime.ofInstant(whenInstant, ZoneId.of("Europe/Prague"))
    //now.getDayOfWeek == startTime.getDayOfWeek &&
    if (
      when.getHour > startTime.getHour &&
        when.getHour < endTime.getHour
    ) {
      true
    } else if (
      when.getHour == startTime.getHour &&
        when.getMinute >= startTime.getMinute
    ) {
      true
    } else if (
      when.getHour == endTime.getHour &&
        when.getMinute <= endTime.getMinute
    ) {
      true
    } else {
      false
    }
  }
}