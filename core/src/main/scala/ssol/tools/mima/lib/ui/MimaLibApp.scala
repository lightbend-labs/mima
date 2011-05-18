package ssol.tools.mima.lib.ui

import ssol.tools.mima.core.ui.MimaSwingApp

object MimaLibApp extends MimaSwingApp {
  override protected def launcherClassName = "ssol.tools.mima.lib.ui.MimaLibApp"
  override val top = new LibFrame
}