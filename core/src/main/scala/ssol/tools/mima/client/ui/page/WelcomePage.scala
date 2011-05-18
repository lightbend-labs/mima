package ssol.tools.mima.client.ui.page

import scala.swing._
import Swing._
import BorderPanel._
import event._

import ssol.tools.mima.core.ui.WithConstraints
import ssol.tools.mima.core.ui.wizard.Wizard

object WelcomePage {
  case object MigrateProject extends Event
  case object MigrateLibrary extends Event
}

class WelcomePage extends GridBagPanel with WithConstraints {

  private val titleText = "Welcome to Scala Migration Manager"

  private val title = new Label(titleText) {
    opaque = false
    font = font.deriveFont(font.getSize2D * 2)
    border = EmptyBorder(20, 0, 0, 0)
    horizontalAlignment = Alignment.Center
  }

  private def createButton(text: String, image: javax.swing.Icon) = {
    val formattedText = text.stripMargin
    val button = new Button(formattedText) {
      icon = image
      opaque = false
      verticalAlignment = Alignment.Top
      horizontalAlignment = Alignment.Left
    }
    listenTo(button)
    button
  }

  private val migrateButtonText = """<html>Mima Client</html>"""
  private val migrate = createButton(migrateButtonText, images.Icons.migration)

  private val checkIncompatibilitiesText = """<html>Mima Lib</html>"""
  private val checkIncompatibilities = createButton(checkIncompatibilitiesText, images.Icons.check)

  import WelcomePage._
  reactions += {
    case ButtonClicked(`migrate`)                => publish(MigrateProject)
    case ButtonClicked(`checkIncompatibilities`) => publish(MigrateLibrary)
  }

  // position elements in GridBagPanel

  import GridBagPanel._
  import java.awt.GridBagConstraints._

  withConstraints(gridy = 0, weightx = 1, fill = Fill.Horizontal, anchor = Anchor.South) {
    add(title, _)
  }

  val ins = new Insets(50, 0, 50, 0)

  withConstraints(gridy = 1, weightx = 1, fill = Fill.Horizontal, insets = ins) {
    add(migrate, _)
  }

  withConstraints(gridy = 2, weightx = 1, fill = Fill.Horizontal, insets = ins) {
    add(checkIncompatibilities, _)
  }

  withConstraints(gridy = 3, weighty = 1, fill = Fill.Both) {
    add(Swing.VGlue, _)
  }
}