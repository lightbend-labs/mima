package ssol.tools.mima.core.ui.widget

import scala.swing.{ Component, GridBagPanel, Label, TextArea, Insets, ScrollPane }
import scala.swing.GridBagPanel.Anchor
import scala.swing.GridBagPanel.Fill
import scala.swing.Swing.{ EmptyBorder, LineBorder }
import scala.swing.Swing
import scala.swing.event.ButtonClicked
import ssol.tools.mima.core.ui.WithConstraints
import java.awt.Color

import ssol.tools.mima.core.Problem

object ProblemInfoView {
	case class Close(source: Component) extends scala.swing.event.Event
}


class ProblemInfoView extends Component {

  private[ProblemInfoView] class Panel extends GridBagPanel with WithConstraints {
    private var lightYellow = new Color(247, 255, 199)
    private val backgroundColor = lightYellow
    background = backgroundColor
    border = EmptyBorder(3)

    val closeButton = new CloseButton
    
    listenTo(closeButton)
    reactions += {
      case ButtonClicked(`closeButton`) =>
        ProblemInfoView.this.publish(new ProblemInfoView.Close(ProblemInfoView.this))
    }

    val statusLabel = new Label("Status:")
    val status = new Label

    val fileLabel = new Label("File:")
    val file = new Label

    val memberLabel = new Label("Member:")
    val member = new Label

    val descriptionLabel = new Label("Description:")
    var description = new TextArea {
      editable = false
      background = backgroundColor
      lineWrap = true
      charWrap = true
    }

    val leftIns = new Insets(0, 9, 10, 5)
    val rightIns = new Insets(0, 0, 10, 9)

    withConstraints(gridwidth = 2, anchor = Anchor.FirstLineEnd, insets = new Insets(0, 0, 0, 12)) {
      add(closeButton, _)
    }

    withConstraints(gridx = 0, gridy = 0, insets = leftIns) {
      add(statusLabel, _)
    }

    withConstraints(gridx = 1, gridy = 0, weightx = 1, insets = rightIns) {
      add(status, _)
    }

    withConstraints(gridx = 0, gridy = 1, insets = leftIns) {
      add(fileLabel, _)
    }

    withConstraints(gridx = 1, gridy = 1, weightx = 1, insets = rightIns) {
      add(file, _)
    }

    withConstraints(gridx = 0, gridy = 2, insets = leftIns) {
      add(memberLabel, _)
    }

    withConstraints(gridx = 1, gridy = 2, weightx = 1, insets = rightIns) {
      add(member, _)
    }

    withConstraints(gridx = 0, gridy = 3, insets = leftIns) {
      add(descriptionLabel, _)
    }

    withConstraints(gridx = 1, gridy = 3, fill = Fill.Horizontal, insets = rightIns) {
      add(description, _)
    }

    withConstraints(gridx = 0, gridy = 4, gridwidth = 2, weightx = 1, weighty = 1, fill = Fill.Both) {
      add(Swing.VGlue, _)
    }
  }

  private lazy val pane = new ScrollPane {
    private val panel = new Panel()
    
    private val view = new Component {
      override lazy val peer = panel.peer
    }

    contents = view

    visible = false

    import javax.swing.ScrollPaneConstants._
    horizontalScrollBarPolicy = new ScrollPane.BarPolicy.Value(HORIZONTAL_SCROLLBAR_NEVER, VERTICAL_SCROLLBAR_AS_NEEDED)
    border = LineBorder(Color.lightGray, 1)

    def updateWith(problem: Problem) = {
      panel.status.text = problem.status.toString
      panel.file.text = problem.fileName
      panel.member.text = problem.referredMember
      panel.description.text = problem.description
    }
  }

  override lazy val peer = pane.peer

  def updateWith(problem: Problem): Unit = { pane updateWith problem }
}