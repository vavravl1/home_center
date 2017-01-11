package entities

import java.time.{Duration, Instant}

import play.api.libs.json._

/**
  * Represents common serialization tools, e.g. instant reader
  */
trait CommonJsonReadWrite {
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
