package loader

import play.api.BuiltInComponents
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import scalikejdbc.config.DBs

/**
  *
  */
trait SqlH2Config extends BuiltInComponents with EvolutionsComponents with DBComponents with HikariCPComponents {
  lazy val initDb = {
    applicationEvolutions
    DBs.setupAll()
  }
}
