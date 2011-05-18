package ssol.tools.mima.core.ui.page

import scala.swing._
import Swing._

import ssol.tools.mima.core.ui.widget.ClassPathEditor
import ssol.tools.mima.core.ui.WithConstraints

class JavaEnvironmentPage extends GridBagPanel with WithConstraints {


  protected val cpEditor = new ClassPathEditor {
    classpathLabel.text = "Make sure that the correct Java environment is selected"
  }
  
  import GridBagPanel._
  import java.awt.GridBagConstraints
  import GridBagConstraints._
  
  
  withConstraints(gridx = 0, gridy = 1, fill = Fill.Both, weightx = 1.0, weighty = 1.0)(add(cpEditor, _))
}