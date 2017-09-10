package entities

import java.time._
import java.time.format.DateTimeFormatter

import play.api.libs.json._

/**
  * Represents common serialization tools, e.g. instant reader
  */
object CommonJsonReadWrite {
  implicit val inWrites = new Writes[Instant] {
    override def writes(in: Instant): JsValue =
      JsNumber(in.toEpochMilli)
  }

  implicit val duWrites = new Writes[Duration] {
    override def writes(d: Duration): JsValue =
      JsNumber(d.toMillis)
  }

  val instantInSecondsReads = new Reads[Instant] {
    override def reads(json: JsValue) = {
      try {
        JsSuccess(Instant.ofEpochSecond(Integer.parseInt(json.toString())))
      } catch {
        case _: Exception => JsError(s"${json.toString()} is not Instant")
      }
    }
  }

  val instantInIso = new Reads[Instant] {
    //2017-09-10 11:10:45
    override def reads(json: JsValue) = {
      try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val parsedDate = LocalDateTime.parse(json.as[String], formatter)

        val instant = Instant.now
        val systemZone = ZoneId.of("Europe/Prague")
        val currentOffsetForMyZone = systemZone.getRules.getOffset(instant)

        JsSuccess(parsedDate.toInstant(currentOffsetForMyZone))
      } catch {
        case _: Exception => JsError(s"${json.toString()} is not Instant")
      }
    }
  }

  implicit val duReads = new Reads[Duration] {
    override def reads(json: JsValue) = {
      try {
        JsSuccess(Duration.ofMillis(Integer.parseInt(json.toString())))
      } catch {
        case _: Exception => JsError(s"${json.toString()} is not Duration")
      }
    }
  }
}
