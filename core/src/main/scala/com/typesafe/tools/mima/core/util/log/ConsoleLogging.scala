package com.typesafe.tools.mima.core.util.log

import com.typesafe.tools.mima.core.Config

private[mima] object ConsoleLogging extends Logging {
  def info(str: String) = if (Config.verbose) println(str)
  def debugLog(str: String) = if (Config.debug) println(str)
  def warn(str: String) = println(str)
  def error(str: String) = Console.err.println(str)
}
