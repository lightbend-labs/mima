package ssol.tools.mima.core.util

import scala.swing._
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import ssol.tools.mima.core.ui.widget.HtmlEditorPane
import ssol.tools.mima.core.ui.Centered

import javax.swing.BorderFactory
import javax.swing.border.TitledBorder

class UnexpectedErrorDialog(error: Throwable)(owner: Window = null) extends Dialog(owner) with Centered {
  assert(error != null)

  title = "Unexpected Error"

  private val explanation = new HtmlEditorPane {
    setText("An unexpected error occurred. Please create a " +
      "<a href=" + Urls.BugReporting + ">new ticket</a> describing " +
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
    }
    override lazy val peer = stackTraceContent.peer
  })

  private val continue = new Button("Continue")

  contents = new BoxPanel(Orientation.Vertical) {
    border = EmptyBorder(10)

    contents += VStrut(10)
    contents += Component.wrap(explanation)
    contents += VStrut(10)
    contents += new FlowPanel(FlowPanel.Alignment.Left)(stackTraceLabel) {vGap = 0; hGap = 0}
    contents += VStrut(10)
    contents += stackTrace
    contents += VStrut(10)
    contents += new FlowPanel(FlowPanel.Alignment.Center)(continue) {vGap = 0; hGap = 0}
  }

  listenTo(continue)
  reactions += {
    case ButtonClicked(`continue`) => dispose()
  }

  location = center
  modal = true
  resizable = false

  pack()

  visible = true
}