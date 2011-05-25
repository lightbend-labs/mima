package ssol.tools.mima.core.ui.page

import scala.swing._
import Swing._
import GridBagPanel._

import ssol.tools.mima.core.ui.model.ReportTableModel
import ssol.tools.mima.core.ui.widget.{ ProblemInfoView, ReportTable, FilterTextField }

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
class ReportPage extends GridBagPanel with WithConstraints {
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

  private val ins = new Insets(0, 0, 10, 0)

  private val filter = new FilterTextField

  withConstraints(insets = ins, gridx = 0, gridy = 0, weightx = .4, fill = Fill.Both)(add(filter, _))

  listenTo(filter)

  reactions += {
    case ValueChanged(`filter`) =>
      try {
        val rf = {
          if (!filter.isDefaultFilterText)
            RegExFilter(filter.text, (0 until table.getModel.getColumnCount): _*)
          else
            NoFilter
        }
        sorter.setRowFilter(rf.rowFilter)
      } catch {
        case e: Exception => () // swallow
      }
  }
  
  private val errorLabel = new Label { foreground = java.awt.Color.RED }

  withConstraints(gridx = 2, gridy = 0, anchor = Anchor.North, insets = new Insets(4, 10, 0, 0)) {
    add(errorLabel, _)
  }

  // the problem table
  private val table = {
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

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1, weightx = 1.0, gridwidth = REMAINDER) {
    add(splitPanel, _)
  }
}

trait TableFilter[T, I] {
  val rowFilter: RowFilter[T, I]
  def &(that: TableFilter[T, I]) = new TableFilter[T, I] { val rowFilter = RowFilter.andFilter(java.util.Arrays.asList(TableFilter.this.rowFilter, that.rowFilter)) }
  def |(that: TableFilter[T, I]) = new TableFilter[T, I] { val rowFilter = RowFilter.orFilter(java.util.Arrays.asList(TableFilter.this.rowFilter, that.rowFilter)) }
  def ~ = new TableFilter[T, I] { val rowFilter = RowFilter.notFilter(TableFilter.this.rowFilter) }
}

object NoFilter extends TableFilter[AbstractTableModel, Integer] {
  val rowFilter = new RowFilter[AbstractTableModel, Integer] {
    def include(entry: RowFilter.Entry[T, I] forSome { type T <: AbstractTableModel; type I <: Integer }) =
      true
  }
}

object RegExFilter {
  def apply(text: String, columns: Int*) = {
    new TableFilter[AbstractTableModel, Integer] {
      val rowFilter: RowFilter[AbstractTableModel, Integer] = RowFilter.regexFilter(escape(text), columns: _*)
    }
  }

  private def escape(str: String): String = {
    str flatMap {
      case '$' => "\\$"
      case '+' => "\\+"
      case c   => c.toString
    }
  }
}

class IgnoreDialog(owner: Window) extends Dialog(owner)