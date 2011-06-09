package ssol.tools.mima.core.util

import java.awt.{ EventQueue, AWTEvent }

import scala.swing.Dialog
import images.Icons

trait WithEventQueueProxy {
  import java.awt.Toolkit
  private val queue = Toolkit.getDefaultToolkit().getSystemEventQueue()
  queue.push(new EventQueueProxy())
}

class EventQueueProxy extends EventQueue {
  override protected def dispatchEvent(newEvent: AWTEvent) {
    try {
      super.dispatchEvent(newEvent)
    } catch {
      case t: Throwable => 
        new BugDialog(t)()
    }
  }
}