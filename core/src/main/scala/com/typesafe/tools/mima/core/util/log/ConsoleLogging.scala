package com.typesafe.tools.mima.core.util.log

private[mima] object ConsoleLogging extends Logging {
  private final val verbose = false
  private final val debug   = false

  def verbose(msg: String) = if (verbose) Console.out.println(msg)
  def debug(msg: String)   = if (debug)   Console.out.println(msg)
  def warn(msg: String)    = Console.out.println(msg)
  def error(msg: String)   = Console.err.println(msg)
}
