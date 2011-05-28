package ssol.tools.mima.lib.ui.widget

import scala.swing.{EditorPane, Component, BorderPanel, CheckBox, Label, FlowPanel}
import scala.swing.event.{ButtonClicked,Event}

object LicenseAgreementView {
  case class LicenseAccepted(status: Boolean) extends Event
}
class LicenseAgreementView extends Component {
  //XXX: without the access modifier scalac can't compile the file... 
  private[LicenseAgreementView] lazy val container = new BorderPanel  {
    add(new FlowPanel(FlowPanel.Alignment.Left)(explanation), BorderPanel.Position.North)
    add(licenseEditor, BorderPanel.Position.Center)
    add(new FlowPanel(FlowPanel.Alignment.Right)(acceptLicense), BorderPanel.Position.South)
  }
  
  override lazy val peer = container.peer
  
  private lazy val explanation = new Label("Please read the following License Agreement.") 
    
  private lazy val licenseText = ""
  private lazy val licenseEditor = new EditorPane("text/html", licenseText) {
    editable = false
  }
  
  private lazy val acceptLicense = new CheckBox("I accept the end user license agreement")
  
  listenTo(acceptLicense)
  
  import LicenseAgreementView.LicenseAccepted
  reactions += {
    case ButtonClicked(`acceptLicense`) =>
      publish(LicenseAccepted(acceptLicense.selected))
  }
  
}