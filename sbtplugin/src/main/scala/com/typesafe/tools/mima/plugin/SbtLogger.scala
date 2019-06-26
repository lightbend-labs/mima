package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core.util.log.Logging
import sbt.Keys._

/** Wrapper on SBT logging for MiMa */
class SbtLogger(s: TaskStreams) extends Logging {
  // MiMa is pretty chatty
  def info(str: String): Unit = s.log.debug(str)
  def debugLog(str: String): Unit = s.log.debug(str)
  def warn(str: String): Unit = s.log.warn(str)
  def error(str: String): Unit = s.log.error(str)
}
