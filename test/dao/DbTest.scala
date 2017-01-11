package dao

import java.io.File

import loader.{DaoConfig, SqlH2Config}
import play.api.routing.Router
import play.api.{Configuration, Environment, Mode}
import play.core.{DefaultWebCommands, SourceMapper, WebCommands}
import scalikejdbc.config.DBs

/**
  * Mix in for test with real daos
  */
trait DbTest extends SqlH2Config with DaoConfig {

  applicationEvolutions
  DBs.setupAll()

  override def sourceMapper: Option[SourceMapper] = None
  override def router: Router = null
  override def environment: Environment = Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test)
  override def webCommands: WebCommands = new DefaultWebCommands
  override def configuration: Configuration = Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test))

}
