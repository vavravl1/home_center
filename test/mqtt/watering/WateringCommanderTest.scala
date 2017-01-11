package mqtt.watering

import java.time.Instant

import entities.watering._
import mqtt.JsonSender
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Writes

class WateringCommanderTest extends WordSpec with Matchers with MockFactory {
  "WateringCommander" when {
    val jsonSender = mock[JsonSender]
    val wateringCommander = new WateringCommander(jsonSender)

    "sends generic command" should {
      "correctly process setting actula time" in {
        val wc = WateringCommand(Set(
          TimeCommand(Instant.ofEpochSecond(42))
        ))
        (jsonSender.send[WateringCommand](_:String, _:WateringCommand)(_:Writes[WateringCommand])).expects("home/watering/ibisek/commands", wc, WateringCommand.comWrites)

        wateringCommander.sendCommand(wc)
      }
    }
  }
}
