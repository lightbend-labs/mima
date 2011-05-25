package ssol.tools.mima.core.ui.widget

import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.event.TableModelListener
import javax.swing.ListSelectionModel

import ssol.tools.mima.core.ui.model.ReportTableModel

private[ui] class ReportTable extends JTable(ReportTableModel(Nil)) with TableModelListener {
  
  // allow only single selection
  setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  
  override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): java.awt.Component = {
    val component = super.prepareRenderer(renderer, row, column)

    component match {
      case jc: javax.swing.JComponent =>
        /** Display a tooltip over each table's element*/
        val cell = getValueAt(row, column)
        val html = htmlFor(cell)
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
  
  override def getModel: ReportTableModel = super.getModel.asInstanceOf[ReportTableModel]
  override def setModel(model: javax.swing.table.TableModel) = {
    assert(model.isInstanceOf[ReportTableModel])
    super.setModel(model)
  }
}