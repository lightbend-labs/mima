package ssol.tools.mima.ui

import scala.swing._
import Swing._


object ui {

  def closeButton = new Button {
    icon = images.Icons.close
    opaque = false
    border = EmptyBorder(0)
    
    import java.awt.Cursor
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
}