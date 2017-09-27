package mqtt.clown

/**
  *
  */
object MeasuredPhenomenonToUnit {
  def apply(sensor:String):String = sensor match {
    case "temperature" => "\u2103"
    case "illuminance" => "lux"
    case "pressure" => "kPa"
    case "altitude" => "m"
    case "concentration" => "ppm"
    case "relative-humidity" => "%"
    case "L1" => "kW"
    case "L2" => "kW"
    case "L3" => "kW"
    case "boiler" => "kW"
    case "L1-surplus" => "kWh"
    case "L1-cons" => "kWh"
    case "L2-cons" => "kWh"
    case "L3-cons" => "kWh"
    case "power" => "W"
    case _ => sensor
  }
}

object MeasuredPhenomenonScale {
  def apply(sensor:String):Double = sensor match {
    case "pressure" => 0.001
    case _ => 1
  }
}
