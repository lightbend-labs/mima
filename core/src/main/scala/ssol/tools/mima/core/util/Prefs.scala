package ssol.tools.mima.core.util

import java.util.prefs.Preferences

object Prefs {
  // Retrieve the user preference node for the package com.mycompany
	private val prefs = Preferences.userNodeForPackage(Prefs.getClass)

	// Preference key name
	private val licenseAccepted: String = "LicenseAccepted";

	def isLicenseAccepted: Boolean = prefs.getBoolean(licenseAccepted, false)
	def beLicenseAccepted() =  prefs.putBoolean(licenseAccepted, true)
}