package com.typesafe.tools.mima.core.util.log

import com.typesafe.tools.mima.core.Config

private[mima] object ConsoleLogging extends Logging {
  def verbose(msg: String) = if (Config.verbose) Console.out.println(msg)
  def debug(msg: String)   = if (Config.debug)   Console.out.println(msg)
  def warn(msg: String)    = Console.out.println(msg)
  def error(msg: String)   = Console.err.println(msg)
}
