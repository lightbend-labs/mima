package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core.util.log.Logging
import sbt.Logger

/** Wrapper on sbt logging for MiMa. */
final private[plugin] class SbtLogger(log: Logger) extends Logging {
  def verbose(msg: String): Unit = log.verbose(msg)
  def debug(msg: String): Unit   = log.debug(msg)
  def warn(msg: String): Unit    = log.warn(msg)
  def error(msg: String): Unit   = log.error(msg)
}
