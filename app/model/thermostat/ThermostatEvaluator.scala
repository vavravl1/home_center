package model.thermostat

import java.time.{Instant, LocalDateTime, ZoneId}

case class SingleSensorThermostatEvaluator(
                                            timeEvaluator: ThermostatTimeEvaluator,
                                            valueEvaluator: ThermostatValueEvaluator
                                          ) {
  def timeMatch(when: Instant): Boolean = timeEvaluator.timeMatch(when)

  def temperatureMatch(temperature: Double) = valueEvaluator.temperatureMatch(temperature: Double)
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
    val when = LocalDateTime.ofInstant(whenInstant, ZoneId.systemDefault())
    //now.getDayOfWeek == startTime.getDayOfWeek &&
    if (
      when.getHour > startTime.getHour &&
        when.getHour < endTime.getHour
    ) {
      true
    } else if(
      when.getHour == startTime.getHour &&
        when.getMinute >= startTime.getMinute
    ) {
      true
    } else if(
      when.getHour == endTime.getHour &&
        when.getMinute <= endTime.getMinute
    ) {
      true
    } else {
      false
    }
  }
}