package dao

import java.time.temporal.ChronoUnit._
import java.time.{Clock, Instant}

import entities.bigclown.{BcMeasure, Location}
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

      bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", i, 10, "C"))
      bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", i.plus(30, MINUTES), 20, "C"))

      bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", i.plus(70, MINUTES), 30, "C"))
      bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", i.plus(80, MINUTES), 30, "C"))
      bcMeasureDao.save(BcMeasure("remote/0", "thermometer", "temperature", i.plus(90, MINUTES), 60, "C"))

      locationDao.saveOrUpdate(Location("remote/0", "upstairs corridor"))

      "correctly samples the temperatures" in {
        (clock.instant _).expects().returning(i).anyNumberOfTimes

        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).average shouldBe 15
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).min shouldBe 10
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).max shouldBe 20
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).measureTimestamp shouldBe i.plus(30, MINUTES)
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).unit shouldBe "C"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).sensor shouldBe "thermometer"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).phenomenon shouldBe "temperature"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).location shouldBe "upstairs corridor"


        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).average shouldBe 40
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).min shouldBe 30
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).max shouldBe 60
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).measureTimestamp shouldBe i.plus(90, MINUTES)
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).unit shouldBe "C"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).sensor shouldBe "thermometer"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).phenomenon shouldBe "temperature"
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).location shouldBe "upstairs corridor"
      }

      "correctly groups the temperatures" in {
        (clock.instant _).expects().returning(i.plus(3, HOURS)).anyNumberOfTimes
        bcMeasureDao.sensorAggregation()
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).average shouldBe 15
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).measureTimestamp shouldBe i.plus(30, MINUTES)
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).min shouldBe 15
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(0).max shouldBe 15

        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).average shouldBe 40
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).measureTimestamp shouldBe i.plus(90, MINUTES)
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).min shouldBe 40
        bcMeasureDao.getSampledMeasures("remote/0", "temperature")(1).max shouldBe 40

        DB.autoCommit(implicit session => {
          sql"""SELECT COUNT(*) FROM bc_measure""".map(rs => rs.int(1)).single.apply() shouldBe Some(2)
        })
      }
    }
  }
}
