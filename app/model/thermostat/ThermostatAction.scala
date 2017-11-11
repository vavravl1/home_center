package model.thermostat

abstract sealed class ThermostatAction
object TurnOn extends ThermostatAction
object TurnOff extends ThermostatAction
object KeepAsIs extends ThermostatAction
