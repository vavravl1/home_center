package entities

import java.time.Instant

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, JsSuccess}

/**
  *
  */
class CommonJsonReadWriteTest extends WordSpec with Matchers {
  "CommonJsonReadWrite" when {
    "given a timestamp" should {
      "parse correctly" in {
        val dateAsString = JsString("2017-09-10 11:10:45")
        CommonJsonReadWrite.instantInIso.reads(dateAsString) shouldBe JsSuccess(Instant.parse("2017-09-10T10:10:45Z"))
      }
    }
  }
}
