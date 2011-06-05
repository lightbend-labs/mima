package ssol.tools.mima.core.ui.widget

import scala.swing.Button
import scala.swing.Swing.EmptyBorder

class CloseButton extends Button {
  icon = images.Icons.close
  opaque = false
  border = EmptyBorder(0)

  import java.awt.Cursor
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
}