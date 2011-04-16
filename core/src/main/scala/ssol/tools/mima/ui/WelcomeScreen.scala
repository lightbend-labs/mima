package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard.Wizard
import BorderPanel._

object WelcomeScreen extends GridBagPanel with WithConstraints {

  val titleText = "Welcome to the Scala bytecode Migration Manager tool"

  val title = new TextArea(titleText) {
    editable = false
    opaque = false
    border = EmptyBorder(20, 12, 0, 0)
  }

  
  
  val migrateButtonText = "Migrate your classes to use a newer library"
  val migrateButton = new Button(migrateButtonText) {
    icon = images.Icons.migration
    opaque = false
    //contentAreaFilled = true
    verticalAlignment = Alignment.Top
    horizontalAlignment = Alignment.Left
  }
  
  val reportButtonText = "Check for incompatibilities between two version of your library".grouped(60).mkString("<html>", "<br>", "</html>")
  val reportButton = new Button(reportButtonText) {
    icon = images.Icons.check
    opaque = false
    //contentAreaFilled = true
    verticalAlignment = Alignment.Top
    horizontalAlignment = Alignment.Left
  }

  import GridBagPanel._
  import java.awt.GridBagConstraints._
  
  withConstraints(gridx = 0, gridy = 0, anchor = Anchor.FirstLineStart, fill = Fill.Horizontal, weightx = 1.0, weighty = 1.0)(add(title, _))
  
  // add buttons
  withConstraints(gridx = 0, gridy = 1, insets = new Insets(10, 10, 60, 10), anchor = Anchor.FirstLineStart, fill = Fill.Horizontal) { c =>
    add(migrateButton, c)
  }
  withConstraints(gridx = 0, gridy = 2, insets = new Insets(10, 10, 0, 10), anchor = Anchor.FirstLineStart, fill = Fill.Horizontal) { c =>
    add(reportButton, c)
  }
  
}