package ssol.tools.mima.core.ui.widget

import javax.swing.SwingUtilities
import javax.swing.{ JTable, PopupFactory }
import java.awt.event.{ MouseEvent, FocusEvent, FocusAdapter }
import scala.swing.Panel

class PopupTableComponent(val contents: Panel) extends java.awt.event.MouseAdapter {

  private lazy val factory = new PopupFactory

  private class HideOnFocusLost(table: JTable) extends FocusAdapter {
    table.addFocusListener(this)

    override def focusLost(e: FocusEvent) {
      hidePopup()
      table.removeFocusListener(this)
    }
  }

  private var popup: javax.swing.Popup = null

  override def mouseReleased(e: java.awt.event.MouseEvent) {
    hidePopup()

    if (SwingUtilities.isRightMouseButton(e)) {
      val source = e.getSource().asInstanceOf[JTable]
      source.addFocusListener(new HideOnFocusLost(source))

      val row = source.rowAtPoint(e.getPoint());
      val column = source.columnAtPoint(e.getPoint());

      if (!source.isRowSelected(row))
        source.changeSelection(row, column, false, false);

      popup = factory.getPopup(e.getComponent(), contents.peer, e.getXOnScreen(), e.getYOnScreen())

      popup.show()
    }
  }

  private def hidePopup() {
    if (popup != null) {
      popup.hide()
    }
  }
}
