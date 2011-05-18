package ssol.tools.mima.client.ui

import scala.swing.Dialog

import ssol.tools.mima.core.ui.MimaFrame
import page.WelcomePage
import ssol.tools.mima.lib.ui.LibWizard
import ssol.tools.mima.core.ui.wizard.Exit

private[ui] class ClientFrame extends MimaFrame {
  val welcome = new WelcomePage
  
  mainContainer.setContent(welcome)

  listenTo(welcome)

  reactions += {
    case WelcomePage.MigrateProject => startWizard(new ClientWizard)

    case WelcomePage.MigrateLibrary => startWizard(new LibWizard)
  }
}