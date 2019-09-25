package com.typesafe.tools.mima.core

import scala.tools.nsc.Settings
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath

object Config {
  val settings: Settings = new Settings
  val verbose: Boolean   = settings.verbose.value
  val debug: Boolean     = settings.debug.value

  var baseClassPath: ClassPath =
    AggregateClassPath.createAggregate(DeprecatedPathApis.newPathResolver(settings).containers: _*)

  lazy val baseDefinitions: Definitions = new Definitions(None, baseClassPath)

  def fatal(msg: String): Nothing = {
    Console.err.println(msg)
    sys.exit(-1)
  }
}
