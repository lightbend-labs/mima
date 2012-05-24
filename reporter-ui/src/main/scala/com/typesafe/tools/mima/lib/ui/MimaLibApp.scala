package com.typesafe.tools.mima.lib.ui

import scala.swing._
import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import com.typesafe.tools.mima.core.ui.{Centered, MimaSwingApp}
import com.typesafe.tools.mima.core.util.Prefs
import com.typesafe.tools.mima.lib.ui.widget.LicenseAgreementView
import com.typesafe.tools.mima.lib.license.License

object MimaLibApp extends MimaSwingApp {
  override protected def launcherClassName = classOf[MimaLibApp].getName

  private class LicenseFrame extends Frame with Centered {
    title = "End User License Agreement"
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
        License.accepted()
        licenseFrame.dispose()
        libFrame.visible = true
      }
    }
  }

  private val licenseAccepted = false
  
  private lazy val licenseFrame = new LicenseFrame
  private lazy val libFrame = new LibFrame
  
  override def top: Frame = if(License.isAccepted) libFrame else licenseFrame 
}

class MimaLibApp