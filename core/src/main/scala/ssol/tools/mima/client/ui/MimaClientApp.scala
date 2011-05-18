package ssol.tools.mima.client.ui

import ssol.tools.mima.core.ui.MimaSwingApp

object MimaClientApp extends MimaSwingApp {
  override protected def launcherClassName = "ssol.tools.mima.client.ui.MimaClientApp"
  override val top = new ClientFrame
}