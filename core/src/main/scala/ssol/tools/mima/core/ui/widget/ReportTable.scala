package ssol.tools.mima.core.ui.widget

import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.event.TableModelListener
import javax.swing.ListSelectionModel

private[ui] class ReportTable extends JTable with TableModelListener {
  // it makes sense to allow only single selection
  setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

  //overriden to set tooltip
  override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): java.awt.Component = {
    val component = super.prepareRenderer(renderer, row, column)

    component match {
      case jc: javax.swing.JComponent =>
        /** Display a tooltip*/
        jc.setToolTipText(getValueAt(row, column).toString.grouped(80).mkString("<html>", "<br>", "</html>"))
      case _ => ()
    }

    component
  }
}