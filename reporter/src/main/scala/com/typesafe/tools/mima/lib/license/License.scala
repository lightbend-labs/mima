package com.typesafe.tools.mima.lib.license

import com.typesafe.tools.mima.core.util.Prefs

object License {
  private val version = "1"

  val license: String = {
    /* The '/' in front is needed for absolutely referencing the resource. */
    val url = getClass.getResourceAsStream("/LICENSE")
    io.Source.fromInputStream(url).mkString
  }

  /** The preference key name */
  private def prefName: String = "LicenseAccepted" + version

  def accepted(): Unit = { Prefs.putBoolean(License.prefName, true) }
  def isAccepted: Boolean = { Prefs.getBoolean(License.prefName, false) }
}
