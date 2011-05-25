package ssol.tools.mima.core.ui.widget

import scala.swing.TextField
import scala.swing.event.{ ValueChanged, FocusGained, FocusLost }

class FilterTextField(val defaultFilterText: String = "<enter filter>") extends TextField(defaultFilterText) {

  def isDefaultFilterText = text == defaultFilterText  
    
  listenTo(this)

  reactions += {
    case FocusGained(_, _, _) if (text == defaultFilterText) =>
      text = ""

    case FocusLost(_, _, _) if (text.trim.isEmpty) =>
      text = defaultFilterText

  }
}