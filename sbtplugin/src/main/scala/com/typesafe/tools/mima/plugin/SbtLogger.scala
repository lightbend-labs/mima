package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core.util.log.Logging
import sbt.Keys._

/** Wrapper on sbt logging for MiMa. */
private[plugin] final class SbtLogger(s: TaskStreams) extends Logging {
  def verbose(msg: String): Unit = s.log.verbose(msg)
  def debug(msg: String): Unit   = s.log.debug(msg)
  def warn(msg: String): Unit    = s.log.warn(msg)
  def error(msg: String): Unit   = s.log.error(msg)
}
