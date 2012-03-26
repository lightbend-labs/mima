package ssol.tools.mima.core.util

import ssol.tools.mima.core.ui.widget.BugReportDialog

trait WithUncaughtExceptionHandlerDialog {
  /** Show a dialog for all uncaught exception. */
  private class UncaughtExceptionHandlerDialog extends Thread.UncaughtExceptionHandler {
    override def uncaughtException(t: Thread, e: Throwable) {
      swing.Swing onEDT {
        try {
        	new BugReportDialog(e)
        }
        catch {
          // Being extremely defensive (avoid running into infinite loop)
          case t: Throwable => t.printStackTrace()
        }
      }
    }
  }

  /** Default exception handler */
  protected val handler: Thread.UncaughtExceptionHandler = new UncaughtExceptionHandlerDialog()
  
  // setting the handler (assumes it is set only once, here - no way to enforce this though)  
  Thread.setDefaultUncaughtExceptionHandler(handler)
}