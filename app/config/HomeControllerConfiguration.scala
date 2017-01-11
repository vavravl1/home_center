package config



/**
  * Holds configuration about connection to the mqtt broker
  */
case class HomeControllerConfiguration(
                     val mqttBrokerUrl: String,
                     val mqttClientId: String
                   )
