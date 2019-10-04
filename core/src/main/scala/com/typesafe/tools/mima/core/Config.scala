package com.typesafe.tools.mima.core

import scala.tools.nsc.Settings

object Config {
  val settings: Settings = new Settings
  val verbose: Boolean   = settings.verbose.value
  val debug: Boolean     = settings.debug.value

  val baseClassPath = DeprecatedPathApis.newPathResolver(Config.settings).result

  def fatal(msg: String): Nothing = {
    Console.err.println(msg)
    sys.exit(-1)
  }
}
