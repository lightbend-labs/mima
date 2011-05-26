package ssol.tools.mima.core.ui.page

import scala.swing._
import Swing._
import GridBagPanel._

import ssol.tools.mima.core.ui.model.ReportTableModel
import ssol.tools.mima.core.ui.widget._

import ssol.tools.mima.core.ui.WithConstraints
import ssol.tools.mima._
import ssol.tools.mima.core.{ Problem, MemberProblem, TemplateProblem }
import ssol.tools.mima.core.ui.widget.CloseButton
import scala.swing.event._

import javax.swing.table._
import javax.swing.RowFilter
import javax.swing.JTable

import java.awt.Color
import java.awt.GridBagConstraints._
import javax.swing.{ JCheckBox, UIManager }
import java.awt.event.{ MouseListener, ItemListener }

/** Mima problem report page. */
class ReportPage extends BorderPanel {
  import javax.swing.event.{ ListSelectionListener, ListSelectionEvent }

  /** When the user selects a row show a description panel for the problem. */
  private class RowSelection(table: ReportTable) extends ListSelectionListener {
    private val LowPanelSplitterHeight = 150

    override def valueChanged(e: ListSelectionEvent) {
      if (e.getValueIsAdjusting) return // skip

      var index = table.getSelectedRow

      val wasVisible = problemInfo.visible
      problemInfo.visible = index >= 0

      if (!problemInfo.visible) {
        splitPanel.dividerLocation = splitPanel.size.height
        return
      }

      updateProblemInfo(index)

      if (!wasVisible) // adjust the split location only if the panel was not visible	  
        splitPanel.dividerLocation = splitPanel.size.height - LowPanelSplitterHeight

      scrollToTop()
    }

    private def updateProblemInfo(rowIndex: Int) {
      val modelRow = table.convertRowIndexToModel(rowIndex)
      problemInfo.updateWith(table.getModel.getProblem(modelRow))
    }

    private def scrollToTop() {
      // scrolling has to be delayed or it won't work, go figure...
      Swing onEDT {
        val origin = (0, 0)
        problemInfo.peer.getViewport.setViewPosition(origin)
      }
    }
  }

  /** Top panel used to define table's filters. When a filter is modified the 
   * table's view is immediately updated. */
  private class TableFiltersPanel(table: ReportTable) extends ListItemsPanel {
    
    type Item = ColumnFilterDef

    private val constraints = new collection.mutable.ArrayBuffer[Item]()

    addConstraint()

    override protected def create() = {
      val fc = new Item(table.getModel.getColumnNames)
      constraints += fc
      listenTo(fc)

      fc
    }

    reactions += {
      case ColumnFilterDef.ColumnFilterDefChanged => refreshTableFilter()
    }

    override protected def remove(c: Item) {
      constraints -= c
      deafTo(c)
      refreshTableFilter()
    }

    def refreshTableFilter() {
      assert(sorter != null, "table's sorter hasn't been initialized")
      val filter = constraints.foldRight[RowFilter[AbstractTableModel, Integer]](NoFilter)((a, b) => RowFilter.andFilter(java.util.Arrays.asList(a.columnFilter, b)))
      sorter.setRowFilter(filter)
    }
  }

  /** table reporting compatibilities problem*/
  private val table = {
    val t = new ReportTable
    val selectionListener = new RowSelection(t)
    t.getSelectionModel.addListSelectionListener(selectionListener)
    t
  }
  
  /** table's filter panel*/
  private val tableFilterPanel = new BorderPanel{
	  private val title = new Label("Filters:")
	  val filters = new TableFiltersPanel(table)
	  
	  add(new BorderPanel {
	    border = EmptyBorder(5,3,0,0)
	    add(title, BorderPanel.Position.North)
	  }, BorderPanel.Position.West)
	  
	  add(filters, BorderPanel.Position.Center)
  }

  /** filter panel is located on the top */
  add(tableFilterPanel, BorderPanel.Position.North)

  /** Make the table scrollable */
  val tableContainer = new ScrollPane(new Component {
    override lazy val peer = table
  })
  
  /** a panel that provides a full description for a given selected problem.
   * class `RowSelectionRowSelection` is responsible of updating the view
   * accordingly to the selected row.
   * */
  private val problemInfo = new ProblemInfoView()

  /** remove row's selection when the problem's description panel is forced to close */
  listenTo(problemInfo)
  reactions += {
    case ProblemInfoView.Close(`problemInfo`) => table.clearSelection()
  }

  /** table sorter. Each time the table's model is updated via `setTableModel` a 
   *  new sorter instance is created and affected. 
   */
  private var sorter: TableRowSorter[AbstractTableModel] = _

  /** Split panel that contains both the table and the problem's description panel. The divider 
   * location is updated by the `RowSelection` class. */
  val splitPanel = new SplitPane(Orientation.Horizontal, tableContainer, problemInfo)

  add(splitPanel, BorderPanel.Position.Center)

  /** A message telling how many unfixable problems have been found */
  private val reportMsg = new Label {
    font = new java.awt.Font("Serif", java.awt.Font.ITALIC, 14)
  }
  
	add(new BorderPanel {
	    border = EmptyBorder(4,0,0,3)
	    add(reportMsg, BorderPanel.Position.East)
	  }, BorderPanel.Position.South)

  
  def setTableModel(_model: ReportTableModel) = {
    reportMsg.visible = _model.getRowCount > 0
    reportMsg.text = "Found " + _model.getRowCount + " incompatibilities (" + _model.countUnfixableProblems + " unfixable / " + _model.countUpgradableProblems +" upgradable)"

    table.setModel(_model)
    table.doLayout

    sorter = new TableRowSorter(_model)
    table.setRowSorter(sorter)
    
    tableFilterPanel.filters.refreshTableFilter()
  }
}


protected[page] object ColumnFilterDef {
  object ColumnFilterDefChanged extends scala.swing.event.Event
}

protected[page] class ColumnFilterDef(_columns: List[String]) extends FlowPanel(FlowPanel.Alignment.Left)() {
  import ColumnFilterDef.ColumnFilterDefChanged
  
  private abstract class TextCombinator {
    def predicate: String => Boolean
  }
  private object Is extends TextCombinator {
    def predicate = _ == filterText
    override def toString = "is"
  }
  private object Contains extends TextCombinator {
    def predicate = _ contains filterText
    override def toString = "contain"
  }
  private object StartsWith extends TextCombinator {
    def predicate = _ startsWith filterText
    override def toString = "startWith"
  }
  private object EndsWith extends TextCombinator {
    def predicate = _ endsWith filterText
    override def toString = "endWith"
  }

  private object Not {
    def apply(c: TextCombinator) = new Not(c)
  }

  private class Not(c: TextCombinator) extends TextCombinator {
    override def predicate = value => !c.predicate(value)

    override def toString = c match {
      case Is => c.toString + " not"
      case _ => "does not " + c.toString
    }
  }

  private class ColumnTextFilter(colIndex: Int, textPredicate: String => Boolean) extends RowFilter[AbstractTableModel, Integer] {
    def include(entry: RowFilter.Entry[T, I] forSome { type T <: AbstractTableModel; type I <: Integer }) = {
      textPredicate(entry.getStringValue(colIndex))
    }
  }

  private val textCombinators = List(Contains, StartsWith, EndsWith, Is,
    Not(Contains), Not(StartsWith), Not(EndsWith), Not(Is))

  private val columns = new ComboBox(_columns)
  private val combinators = new ComboBox(textCombinators)
  private val value = new TextField(20)

  private def filterText = value.text.trim

  listenTo(columns.selection, combinators.selection, value)

  reactions += {
    case scala.swing.event.SelectionChanged(_) =>
      publish(ColumnFilterDefChanged)
    case scala.swing.event.ValueChanged(`value`) =>
      publish(ColumnFilterDefChanged)
  }

  vGap = 0

  contents ++= Seq(columns, combinators, value)

  def columnFilter: RowFilter[AbstractTableModel, Integer] =
    if (filterText.isEmpty)
      NoFilter
    else
      new ColumnTextFilter(columns.selection.index, combinators.selection.item.predicate)

}

protected[page] object NoFilter extends RowFilter[AbstractTableModel, Integer] {
  def include(entry: RowFilter.Entry[T, I] forSome { type T <: AbstractTableModel; type I <: Integer }) =
    true
}
