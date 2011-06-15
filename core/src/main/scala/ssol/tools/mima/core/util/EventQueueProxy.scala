package ssol.tools.mima.core.util

import java.awt.{ EventQueue, AWTEvent }

import scala.swing.Dialog
import ssol.tools.mima.core.ui.widget.BugReportDialog

/** Replace the default event queue with a customized one that 
 * enables to recover from unexpected exceptions */
trait WithEventQueueProxy {

  /** Specialized event queue that catches all unexpected exceptions and
   *  opens a UI dialog for reporting the error.
   */
  private class EventQueueProxy extends EventQueue {
    override protected def dispatchEvent(newEvent: AWTEvent) {
      try {
        super.dispatchEvent(newEvent)
      } catch {
        case t: Throwable =>
          new BugReportDialog(t)()
      }
    }
  }
  
  import java.awt.Toolkit
  private val queue = Toolkit.getDefaultToolkit().getSystemEventQueue()
  queue.push(new EventQueueProxy())
}