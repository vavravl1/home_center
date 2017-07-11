package loader

import java.time.Clock

import play.api.BuiltInComponents

/**
  * Defines app clock
  */
trait ClockConfig extends BuiltInComponents {
  lazy val clock = Clock.systemUTC()
}
