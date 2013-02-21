package com.typesafe.tools.mima.core.util.log

trait Logging {
  def info(str: String): Unit
  def debugLog(str: String): Unit
}