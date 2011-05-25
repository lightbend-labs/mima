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
class ReportPage extends BorderPanel /*GridBagPanel with WithConstraints*/ {
  import javax.swing.event.{ ListSelectionListener, ListSelectionEvent }

  /** When the user selects a row show a description panel for the problem. */
  private[ReportPage] class RowSelection(table: ReportTable) extends ListSelectionListener {
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

  private[ReportPage] class FilterConstraint extends FlowPanel(FlowPanel.Alignment.Left)() {
    vGap = 0
    abstract class Comparer {
      def comparer: String => Boolean
    }
    object Is extends Comparer {
      def comparer = _ == value.text
      override def toString = "is"
    }
    object Contains extends Comparer {
      def comparer = _ contains value.text
      override def toString = "contains"
    }
    object StartsWith extends Comparer {
      def comparer = _ startsWith value.text
      override def toString = "startsWith"
    }
    object EndsWith extends Comparer {
      def comparer = _ endsWith value.text
      override def toString = "endsWith"
    }
    
    object Not{
      def apply(c: Comparer) = new Not(c)
    }
    
    class Not(c: Comparer) extends Comparer {
      override def comparer = value => !c.comparer(value) 
      
      override def toString = c.toString + " not"
    }

    private val verbs = List(Contains, StartsWith, EndsWith, Is, 
        							Not(Contains), Not(StartsWith), Not(EndsWith), Not(Is))

    private val listNames = new ComboBox(table.getModel.getColumnNames)
    private val listVerbs = new ComboBox(verbs)
    val value = new TextField(40)

    def createColumnTextFilter: RowFilter[AbstractTableModel, Integer] =
      if(value.text.trim.isEmpty)
        NoFilter
      else
      	new ColumnTextFilter(listNames.selection.index, listVerbs.selection.item.comparer)

    contents ++= Seq(listNames, listVerbs, value)
  }

  class FilterConstraintsPanel extends ListItemsPanel {
    type Item = FilterConstraint
    private val constraints = new collection.mutable.ArrayBuffer[FilterConstraint]()

    addConstraint()

    override protected def create() = {
      val fc = new FilterConstraint
      constraints += fc
      listenTo(fc.value)

      fc
    }

    reactions += {
      case ValueChanged(_) =>
        computeFilter()
    }

    override protected def remove(c: FilterConstraint) {
      constraints -= c
      deafTo(c)
      computeFilter()
    }

    private def computeFilter() {
      val filter = constraints.foldRight[RowFilter[AbstractTableModel, Integer]](NoFilter)((a, b) => RowFilter.andFilter(java.util.Arrays.asList(a.createColumnTextFilter, b)))
      sorter.setRowFilter(filter)
    }
  }

  private val constraintsPanel = new FilterConstraintsPanel()
  add(constraintsPanel, BorderPanel.Position.North)

  private val errorLabel = new Label { foreground = java.awt.Color.RED }

  add(errorLabel, BorderPanel.Position.South)

  // the problem table
  private lazy val table = {
    val t = new ReportTable
    val selectionListener = new RowSelection(t)
    t.getSelectionModel.addListSelectionListener(selectionListener)
    t
  }

  val tableContainer = new ScrollPane(new Component {
    override lazy val peer = table
  })

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def setTableModel(_model: ReportTableModel) = {
    errorLabel.visible = _model.hasUnfixableProblems
    errorLabel.text = "There are " + _model.countUnfixableProblems + " unfixable incompatibilities"

    table.setModel(_model)
    table.doLayout

    sorter = new TableRowSorter(_model)
    table.setRowSorter(sorter)
  }

  private val problemInfo = new ProblemInfoView()

  listenTo(problemInfo)

  reactions += {
    case ProblemInfoView.Close(`problemInfo`) => table.clearSelection()
  }

  val splitPanel = new SplitPane(Orientation.Horizontal, tableContainer, problemInfo)

  add(splitPanel, BorderPanel.Position.Center)
}

object NoFilter extends RowFilter[AbstractTableModel, Integer] {
  def include(entry: RowFilter.Entry[T, I] forSome { type T <: AbstractTableModel; type I <: Integer }) =
    true
}

class ColumnTextFilter(colIndex: Int, pred: String => Boolean) extends RowFilter[AbstractTableModel, Integer] {
  def include(entry: RowFilter.Entry[T, I] forSome { type T <: AbstractTableModel; type I <: Integer }) = {
    pred(entry.getStringValue(colIndex))
  }
}
