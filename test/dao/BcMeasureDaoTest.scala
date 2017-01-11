package dao

import java.time.temporal.ChronoUnit._
import java.time.{Clock, Instant}

import entities.bigclown.BcMeasure
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import scalikejdbc._

/**
  *
  */
class BcMeasureDaoTest extends WordSpec with Matchers with DbTest with MockFactory {
  override lazy val clock = mock[Clock]

  "BcMeasureDao" when {
    "filled by measures over last 3 hours" should {
      val i = Instant.ofEpochSecond(0)
      bcMeasureDao.cleanDb()

      bcMeasureDao.save(BcMeasure("thermometer", "temperature", i, 10, "C"))
      bcMeasureDao.save(BcMeasure("thermometer", "temperature", i.plus(30, MINUTES), 20, "C"))

      bcMeasureDao.save(BcMeasure("thermometer", "temperature", i.plus(70, MINUTES), 30, "C"))
      bcMeasureDao.save(BcMeasure("thermometer", "temperature", i.plus(80, MINUTES), 30, "C"))
      bcMeasureDao.save(BcMeasure("thermometer", "temperature", i.plus(90, MINUTES), 60, "C"))

      "correctly samples the temperatures" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes

        bcMeasureDao.getSampledMeasures("temperature")(0).value shouldBe 15
        bcMeasureDao.getSampledMeasures("temperature")(0).measureTimestamp shouldBe i.plus(30, MINUTES)
        bcMeasureDao.getSampledMeasures("temperature")(0).unit shouldBe "C"
        bcMeasureDao.getSampledMeasures("temperature")(0).sensor shouldBe "thermometer"
        bcMeasureDao.getSampledMeasures("temperature")(0).phenomenon shouldBe "temperature"

        bcMeasureDao.getSampledMeasures("temperature")(1).value shouldBe 40
        bcMeasureDao.getSampledMeasures("temperature")(1).measureTimestamp shouldBe i.plus(90, MINUTES)
        bcMeasureDao.getSampledMeasures("temperature")(1).unit shouldBe "C"
        bcMeasureDao.getSampledMeasures("temperature")(1).sensor shouldBe "thermometer"
        bcMeasureDao.getSampledMeasures("temperature")(1).phenomenon shouldBe "temperature"
      }

      "correctly groups the temperatures" in {
        (clock.instant _).expects().returning(i.plus(130, MINUTES)).anyNumberOfTimes
        bcMeasureDao.sensorAggregation()
        bcMeasureDao.getSampledMeasures("temperature")(0).value shouldBe 15
        bcMeasureDao.getSampledMeasures("temperature")(0).measureTimestamp shouldBe i.plus(30, MINUTES)
        bcMeasureDao.getSampledMeasures("temperature")(1).value shouldBe 40
        bcMeasureDao.getSampledMeasures("temperature")(1).measureTimestamp shouldBe i.plus(90, MINUTES)

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM bc_measure""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }
  }
}
