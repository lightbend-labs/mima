package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard.Wizard
import BorderPanel._
import event._

object WelcomeScreen extends GridBagPanel with WithConstraints {
  case object MigrateBinaries extends Event
  case object CheckIncompatibilities extends Event 
  
  // create ui elements
  
  private val titleText = "Welcome to the Scala bytecode Migration Manager tool"

  private val title = new TextArea(titleText) {
    editable = false
    opaque = false
    border = EmptyBorder(20, 12, 0, 0)
  }

  private def createButton(text: String, image: javax.swing.Icon) = {
    val button = new Button(text) {
      icon = image
      opaque = false
      verticalAlignment = Alignment.Top
      horizontalAlignment = Alignment.Left
    }
    listenTo(button)
    button
  }

  private val migrateButtonText = "Migrate your classes to use a newer library"
  private val migrate = createButton(migrateButtonText, images.Icons.migration)

  private val checkIncompatibilitiesText = "Check for incompatibilities between two version of your library".grouped(60).mkString("<html>", "<br>", "</html>")
  private val checkIncompatibilities = createButton(checkIncompatibilitiesText, images.Icons.check)

  private val exit = new Button("Quit")
  listenTo(exit)
  
  reactions += {
    case ButtonClicked(`migrate`) => publish(MigrateBinaries)
    case ButtonClicked(`checkIncompatibilities`) => publish(CheckIncompatibilities)
    case ButtonClicked(`exit`) => publish(Exit)
  }
  
  // position elements in GridBagPanel
  
  import GridBagPanel._
  import java.awt.GridBagConstraints._

  private val c = new Constraints

  c.fill = Fill.Horizontal // expands added elements to fill the horizontal space
  c.gridx = 0 // grid position in cartesian coordinates (x,y)
  c.gridy = 0
  c.weightx = 1 // fill horizontal space when the container is resized
  c.weighty = 0.1 // this puts some space between the title and the next element 
  c.anchor = Anchor.North
  layout(title) = c // apply constraints to title

  c.weighty = 0 // reset vertical space (following elements will keep fixed distance)
  c.insets = new Insets(0, 8, 40, 0) // margin (top, left, bottom, right) 
  c.gridy = 1 // position element in the second row (0,1)

  layout(migrate) = c

  c.gridy = 2 // position element in the third row (0,2)
  layout(checkIncompatibilities) = c
  
  c.fill = Fill.None
  c.insets = new Insets(0,0,0,0)
  c.gridy = 3
  c.weightx = 0
  c.anchor = Anchor.SouthEast
  layout(exit) = c
}