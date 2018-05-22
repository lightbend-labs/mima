package com.typesafe.tools.mima.core.util.log

trait Logging {
  def info(str: String): Unit
  def debugLog(str: String): Unit
  def warn(str: String): Unit
  def error(str: String): Unit
}
