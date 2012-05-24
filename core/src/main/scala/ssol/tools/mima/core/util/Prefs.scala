package ssol.tools.mima.core.util

import java.util.prefs.Preferences

/**
 * Minimalistic wrapper around the Java preference store.
 *
 * This class is thread-safe (and that because the underlying
 * {{{java.util.prefs.Preferences}}} is thread-safe).
 */
object Prefs {
  // Retrieve the user preference node for the package com.mycompany
  private val prefs = Preferences.userNodeForPackage(Prefs.getClass)

  def getBoolean(key: String, orElse: Boolean): Boolean = prefs.getBoolean(key, orElse)

  def putBoolean(key: String, value: Boolean): Unit = prefs.putBoolean(key, value)
}