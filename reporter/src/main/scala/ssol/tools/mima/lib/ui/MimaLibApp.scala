package ssol.tools.mima.lib.ui

import scala.swing._
import scala.swing.Swing._
import scala.swing.event.ButtonClicked

import ssol.tools.mima.core.ui.{Centered, MimaSwingApp}
import ssol.tools.mima.core.util.Prefs
import ssol.tools.mima.lib.ui.widget.LicenseAgreementView

import ssol.tools.mima.lib.license.License

object MimaLibApp extends MimaSwingApp {
  override protected def launcherClassName = "ssol.tools.mima.lib.ui.MimaLibApp"

  private class LicenseFrame extends Frame with Centered {
    title = "Typesafe - End User License Agreement"
    preferredSize = (640, 480)
    minimumSize = preferredSize
    location = center
    resizable = false
    
    private val license = new LicenseAgreementView(License.license)
    private val continue = new Button("Continue") {
      enabled = false
    }
    
    contents = new BorderPanel {
      border = EmptyBorder(3)
    	add(license, BorderPanel.Position.Center)
      add(new FlowPanel(FlowPanel.Alignment.Right)(continue), BorderPanel.Position.South)
    }
    
    listenTo(license, continue)
    
    reactions += {
      case LicenseAgreementView.LicenseAccepted(value) => continue.enabled = value
      case ButtonClicked(`continue`) => {
        Prefs.beLicenseAccepted()
        licenseFrame.dispose
        libFrame.visible = true
      }
    }
  }

  private val licenseAccepted = false
  
  private lazy val licenseFrame = new LicenseFrame
  private lazy val libFrame = new LibFrame
  
  override def top: Frame = if(Prefs.isLicenseAccepted) libFrame else licenseFrame 
}