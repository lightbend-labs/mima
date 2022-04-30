package com.typesafe.tools.mima.core.util.log

private[mima] object ConsoleLogging extends Logging {
  final private val debug = false

  def verbose(msg: String) = debug(msg)
  def debug(msg: String)   = if (debug) Console.out.println(msg)
  def warn(msg: String)    = Console.out.println(msg)
  def error(msg: String)   = Console.err.println(msg)
}
