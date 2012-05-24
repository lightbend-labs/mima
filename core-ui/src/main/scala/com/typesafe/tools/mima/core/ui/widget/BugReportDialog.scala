package com.typesafe.tools.mima.core.ui.widget

import scala.swing._
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import com.typesafe.tools.mima.core.ui.Centered

import com.typesafe.tools.mima.core.util.Urls

import javax.swing.BorderFactory
import javax.swing.border.TitledBorder

/** Simple dialog for reporting unexpected failures.
 *  The dialog is `Centered` in the screen.
 *  The dialog contains
 *    (1) An hyperlink to the bug reporting site
 *    (2) A textarea containing the exception's stack trace
 *    (3) A `continue` button to close the dialog
 *
 *  @precondition: the `error` must not be null
 */
class BugReportDialog(error: Throwable, owner: Window = null) extends Dialog(owner) with Centered {
  assert(error != null)

  title = "Unexpected Error"

  /** Short message explaining the user what he should do to report the error.*/
  private val explanation = new HtmlViewPane {
    setHtml("An unexpected error occurred. Please create a " +
      "<a href=" + Urls.BugReportingSite + ">new ticket</a> describing " +
      "the issue.")
  }

  private val stackTraceLabel = new Label {
    text = "Please, make sure to copy/paste the stack trace when creating the bug report"
  }

  private val stackTrace = new ScrollPane(new Component {
    private lazy val stackTraceContent = new TextArea {
      background = java.awt.Color.WHITE
      editable = false
      text = error.getStackTrace.mkString("\n")
      wordWrap = true
      lineWrap = true
      rows = 10 // make sure the dialog size is fixed!
    }
    override lazy val peer = stackTraceContent.peer
  })

  /** The continue button is used to dispose the dialog */
  private val continue = new Button("Continue")

  listenTo(continue)
  reactions += {
    case ButtonClicked(`continue`) => dispose()
  }

  /** lay out all UI elements vertically */
  contents = new BoxPanel(Orientation.Vertical) {
    border = EmptyBorder(10)

    contents += VStrut(10)
    contents += explanation
    contents += VStrut(10)
    contents += new FlowPanel(FlowPanel.Alignment.Left)(stackTraceLabel) { vGap = 0; hGap = 0 }
    contents += VStrut(10)
    contents += stackTrace
    contents += VStrut(10)
    contents += new FlowPanel(FlowPanel.Alignment.Center)(continue) { vGap = 0; hGap = 0 }
  }

  location = center
  modal = true
  resizable = false 
  pack()
  visible = true
}
  