package com.typesafe.tools.mima.lib.ui.widget

import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.event.TableModelListener
import javax.swing.ListSelectionModel

import com.typesafe.tools.mima.lib.ui.model.ReportTableModel

private[ui] class ReportTable extends JTable(ReportTableModel(Nil)) with TableModelListener {
  
  // allow only single selection
  setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

  
  // turn off grid as we use the `AlternatedColoredRowRenderer`
  setShowGrid(false)

  // apply the render to all cells! 
  setDefaultRenderer(classOf[Any], new AlternatedColoredRowRenderer)

  // remove any space between columns and rows 
  setIntercellSpacing(new java.awt.Dimension(0, 0))

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
    if (value == null)
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

import javax.swing.table.DefaultTableCellRenderer
import java.awt.Color

private object AlternatedColoredRowRenderer {
  val WhiteSmoke = new java.awt.Color(245, 245, 245)
  val Azure = new java.awt.Color(240, 240, 255)
}

class AlternatedColoredRowRenderer(oddRowsColor: Color = AlternatedColoredRowRenderer.Azure, evenRowsColor: Color = Color.white)
  extends DefaultTableCellRenderer {

  override def getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): java.awt.Component = {
    // ignoring focus so that the cell is drawn with no border.
    val ignoreFocus = false
    val component = super.getTableCellRendererComponent(table, value, isSelected, ignoreFocus, row, column)

    if (isSelected) return component

    if (row % 2 == 0) {
      component.setBackground(evenRowsColor)
    } else {
      component.setBackground(oddRowsColor)
    }

    component
  }
}