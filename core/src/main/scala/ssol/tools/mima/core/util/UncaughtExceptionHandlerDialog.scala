package ssol.tools.mima.core.util

import ssol.tools.mima.core.ui.widget.BugReportDialog

trait WithUncaughtExceptionHandlerDialog {
  Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerDialog())
}

class UncaughtExceptionHandlerDialog extends Thread.UncaughtExceptionHandler {

  override def uncaughtException(t: Thread, e: Throwable) {
    new BugReportDialog(e)()
  }
}