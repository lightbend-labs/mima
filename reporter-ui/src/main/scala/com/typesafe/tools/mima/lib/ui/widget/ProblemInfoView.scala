package com.typesafe.tools.mima.lib.ui.widget

import scala.swing._
import scala.swing.Swing.{ EmptyBorder, LineBorder }
import scala.swing.GridBagPanel.Anchor
import scala.swing.GridBagPanel.Fill
import scala.swing.event.ButtonClicked
import com.typesafe.tools.mima.core.ui.WithConstraints
import com.typesafe.tools.mima.core.ui.widget.CloseButton
import java.awt.Color

import com.typesafe.tools.mima.core.Problem

object ProblemInfoView {
  case class Close(source: Component) extends scala.swing.event.Event
}

class ProblemInfoView extends Component {

  private[ProblemInfoView] class Panel extends GridBagPanel with WithConstraints {
    opaque = false

    val fileLabel = new Label("File:") { xAlignment = Alignment.Left }
    val file = new Label { xAlignment = Alignment.Left }

    val memberLabel = new Label("Member:") { xAlignment = Alignment.Left }
    val member = new Label { xAlignment = Alignment.Left }

    val descriptionLabel = new Label("Description:") { xAlignment = Alignment.Left }
    var description = new EditorPane() {
      opaque = false
      editable = false
    }

    val leftIns = new Insets(0, 9, 10, 10)
    val rightIns = new Insets(0, 0, 10, 9)

    withConstraints(gridwidth = 2, anchor = Anchor.FirstLineEnd, insets = new Insets(0, 0, 0, 12)) {
      add(closeButton, _)
    }

    withConstraints(gridx = 0, gridy = 0, insets = leftIns) {
      add(fileLabel, _)
    }

    withConstraints(gridx = 1, gridy = 0, weightx = 1, insets = rightIns) {
      add(file, _)
    }

    withConstraints(gridx = 0, gridy = 1, insets = leftIns) {
      add(memberLabel, _)
    }

    withConstraints(gridx = 1, gridy = 1, weightx = 1, insets = rightIns) {
      add(member, _)
    }

    withConstraints(gridx = 0, gridy = 2, insets = leftIns) {
      add(descriptionLabel, _)
    }

    withConstraints(gridx = 1, gridy = 2, fill = Fill.Horizontal, insets = rightIns) {
      add(description, _)
    }

    withConstraints(gridx = 0, gridy = 3, weighty = 1, fill = Fill.Both) {
      add(Swing.VGlue, _)
    }

  }

  private lazy val LightYellow = new Color(247, 255, 199)

  private lazy val closeButton = new CloseButton {
    opaque = false
    background = LightYellow
  }

  listenTo(closeButton)
  reactions += {
    case ButtonClicked(`closeButton`) =>
      ProblemInfoView.this.publish(new ProblemInfoView.Close(ProblemInfoView.this))
  }

  private lazy val container = new BorderPanel {


    background = LightYellow
    border = EmptyBorder(3)
    val infoPanel = new Panel()
    add(infoPanel, BorderPanel.Position.Center)
    add(new BorderPanel {
      opaque = false
      border = EmptyBorder(0,0,0,12)
      add(closeButton, BorderPanel.Position.North)
    }, BorderPanel.Position.East)
  }

  private lazy val pane = new ScrollPane {

    private val view = new Component {
      override lazy val peer = container.peer
    }

    contents = view

    visible = false

    import javax.swing.ScrollPaneConstants._
    horizontalScrollBarPolicy = new ScrollPane.BarPolicy.Value(HORIZONTAL_SCROLLBAR_NEVER, VERTICAL_SCROLLBAR_AS_NEEDED)
    border = LineBorder(Color.lightGray, 1)

    def updateWith(problem: Problem) = {
      container.infoPanel.file.text = problem.fileName
      container.infoPanel.member.text = problem.referredMember
      container.infoPanel.description.text = problem.description
    }
  }

  override lazy val peer = pane.peer

  def updateWith(problem: Problem): Unit = { pane updateWith problem }
}