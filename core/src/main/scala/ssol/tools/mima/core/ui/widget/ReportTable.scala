package ssol.tools.mima.core.ui.widget

import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.event.TableModelListener
import javax.swing.ListSelectionModel

private[ui] class ReportTable extends JTable with TableModelListener {
  // allow only single selection
  setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  
  override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): java.awt.Component = {
    val component = super.prepareRenderer(renderer, row, column)

    component match {
      case jc: javax.swing.JComponent =>
        /** Display a tooltip over each table's element*/
        val element = getValueAt(row, column)
        val html = htmlFor(element)
        jc.setToolTipText(html)
      case _ => ()
    }

    component
  }
  
  private def htmlFor(value: Any): String = {
    if(value == null)
      ""
    else
      value.toString.grouped(80).mkString("<html>", "<br>", "</html>")
  }
}