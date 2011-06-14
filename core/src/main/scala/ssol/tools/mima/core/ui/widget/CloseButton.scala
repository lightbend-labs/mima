package ssol.tools.mima.core.ui.widget

import scala.swing.Button
import scala.swing.Swing.EmptyBorder

import ssol.tools.mima.core.ui.images

/** A close button displayed as an icon. When the mouse is over 
 * the image a hand cursor is shown.*/
class CloseButton extends Button {
  icon = images.Icons.close
  opaque = false
  border = EmptyBorder(0)

  import java.awt.Cursor
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
}