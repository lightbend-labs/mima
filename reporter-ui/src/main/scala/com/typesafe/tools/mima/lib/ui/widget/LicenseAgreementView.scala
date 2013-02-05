package com.typesafe.tools.mima.lib.ui.widget

import scala.swing.{ EditorPane, Component, BorderPanel, CheckBox, Label, FlowPanel, ScrollPane, Dimension }
import scala.swing.Swing.{ EmptyBorder, LineBorder }
import scala.swing.event.{ ButtonClicked, Event }

object LicenseAgreementView {
  case class LicenseAccepted(status: Boolean) extends Event
}

class LicenseAgreementView(licenseText: String) extends Component {
  private lazy val container = new BorderPanel {
    add(new FlowPanel(FlowPanel.Alignment.Left)(explanation) {hGap = 0; vGap = 3}, BorderPanel.Position.North)
    add(new ScrollPane {
      contents = licenseEditor
      horizontalScrollBarPolicy = new ScrollPane.BarPolicy.Value(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

      peer.addComponentListener(new java.awt.event.ComponentAdapter {
        override def componentResized(e: java.awt.event.ComponentEvent) {
          licenseEditor.peer setSize new Dimension(size.width - 20, size.height)
        }
      });

    }, BorderPanel.Position.Center)
    add(new FlowPanel(FlowPanel.Alignment.Right)(acceptLicense) {hGap = 0; vGap = 0}, BorderPanel.Position.South)
  }

  override lazy val peer: javax.swing.JComponent = container.peer

  private lazy val explanation = new Label("Please read the following End User License Agreement.")

  private lazy val licenseEditor = new Component {
	border = EmptyBorder(5)
	private lazy val pane = new EditorPane("text/plain", licenseText) {
      editable = false
      focusable = false
    }
    override lazy val peer = pane.peer
  }

  private lazy val acceptLicense = new CheckBox("Accept the End User License Agreement")

  listenTo(acceptLicense)

  import LicenseAgreementView.LicenseAccepted
  reactions += {
    case ButtonClicked(`acceptLicense`) =>
      publish(LicenseAccepted(acceptLicense.selected))
  }

}
