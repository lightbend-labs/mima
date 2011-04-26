package ssol.tools.mima
package ui

import scala.swing._
import Swing._
import GridBagPanel._
import scala.swing.event._

import javax.swing.table._
import javax.swing.RowFilter
import javax.swing.JTable

import java.awt.Color
import java.awt.GridBagConstraints._
import javax.swing.{ JCheckBox, UIManager }
import java.awt.event.{ MouseListener, ItemListener }

/** Mima problem report page.
 */
class ReportPage extends GridBagPanel with WithConstraints {
  import javax.swing.event._
  import javax.swing._

  /** Show problem description panel when the user selects a row. */
  class RowSelection(table: JTable) extends ListSelectionListener {
    override def valueChanged(e: ListSelectionEvent) {
      var index = table.getSelectedRow

      if (e.getValueIsAdjusting) return // skip

      if (index >= 0) {
        val modelRow = table.convertRowIndexToModel(index)
        problemPanel.problem_=(table.getModel.getValueAt(modelRow, ProblemsModel.ProblemDataColumn).asInstanceOf[Problem])
      }

      problemPanel.visible = index >= 0
    }
  }

  protected class ReportTable extends JTable with TableModelListener {
    /** Special renderer for unfixable issues, checkbox is rendered as "-" */
    object UnavailableFixCellRenderer extends DefaultTableCellRenderer {
      private val UnavailableFix = "-"
      
      override def getTableCellRendererComponent(table: JTable, value: AnyRef, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): java.awt.Component = {
        super.getTableCellRendererComponent(table, UnavailableFix, isSelected, hasFocus, row, column)
      }
      
      setHorizontalAlignment(javax.swing.SwingConstants.CENTER)
    }

    // it makes sense to allow only single selection
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

    //overriden to set tooltip
    override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): java.awt.Component = {
      val component = super.prepareRenderer(renderer, row, column)

      component match {
        case jc: javax.swing.JComponent =>
          /** Display a tooltip*/
          jc.setToolTipText(table.getValueAt(row, column).toString.grouped(80).mkString("<html>", "<br>", "</html>"))
        case _ => ()
      }

      component
    }
  }

  private val ins = new Insets(0, 10, 10, 10)

  private val defaultFilterText = "<enter filter>"
  private val filter = new TextField(defaultFilterText)

  withConstraints(insets = ins, gridx = 0, gridy = 0, weightx = .4, fill = Fill.Both)(add(filter, _))

  private val errorLabel = new Label("Unfortunately there are unfixable incompatibilities") {
    foreground = java.awt.Color.RED
  }
  withConstraints(insets = new Insets(4, 10, 10, 10), gridx = 2, gridy = 0, anchor = Anchor.North) {
    add(errorLabel, _)
  }

  // the problem table
  private val table = new ReportTable
  val selectionListener = new RowSelection(table)
  table.getSelectionModel.addListSelectionListener(selectionListener)
  
  // when the table loose its focus hide the problem description panel
  table.addFocusListener(new java.awt.event.FocusAdapter {
    override def focusLost(e: java.awt.event.FocusEvent) {
      table.getSelectionModel.clearSelection()
    }
  })

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 0.6, weightx = 1.0, gridwidth = REMAINDER, insets = ins) {
    add(new ScrollPane(new Component {
      override lazy val peer = table
    }), _)
  }

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def setTableModel(_model: ProblemsModel) = {
    errorLabel.visible = _model.hasUnfixableProblems

    table.setModel(_model)
    table.getColumnModel.getColumn(0).setPreferredWidth(50)
    table.getColumnModel.getColumn(1).setPreferredWidth(100)
    table.getColumnModel.getColumn(2).setPreferredWidth(150)

    sorter = new TableRowSorter(_model)
    table.setRowSorter(sorter)
  }

  // filtering
  listenTo(filter)

  reactions += {
    case ValueChanged(`filter`) =>
      try {
        val rf: RowFilter[AbstractTableModel, Integer] = if (filter.text != defaultFilterText) RowFilter.regexFilter(escape(filter.text), 1, 2) else RowFilter.regexFilter("*")
        sorter.setRowFilter(rf)
      } catch {
        case _ => () // swallow any illegal regular expressions
      }
    case FocusGained(`filter`, _, _) if (filter.text == defaultFilterText) =>
      filter.text = ""

    case FocusLost(`filter`, _, _) if (filter.text.trim.isEmpty) =>
      filter.text = defaultFilterText

  }

  private def escape(str: String): String = {
    str flatMap {
      case '$' => "\\$"
      case '+' => "\\+"
      case c   => c.toString
    }
  }

  private val problemPanel = new GridBagPanel with WithConstraints {
    private val backgroundColor = new Color(247, 255, 199) // light-yellow
    visible = false
    background = backgroundColor
    border = LineBorder(Color.lightGray, 1)

    private var _problem: Problem = _
    def problem_=(problem: Problem) = {
      status.text = problem.status.toString
      member.text = ProblemsModel.getReferredMember(problem)
      description.text = problem.description
    }

    val statusLabel = new Label("Status:")
    val status = new Label
    val memberLabel = new Label("Member:")
    val member = new Label

    val descriptionLabel = new Label("Description:")
    var description = new TextArea {
      editable = false
      background = backgroundColor
      lineWrap = true
      charWrap = true
    }

    val ins = new Insets(5, 5, 5, 5)

    withConstraints(gridx = 0, gridy = 0, insets = ins) {
      add(statusLabel, _)
    }

    withConstraints(gridx = 1, gridy = 0, weightx = 1, insets = ins) {
      add(status, _)
    }

    withConstraints(gridx = 0, gridy = 1, insets = ins) {
      add(memberLabel, _)
    }

    withConstraints(gridx = 1, gridy = 1, weightx = 1, insets = ins) {
      add(member, _)
    }

    withConstraints(gridx = 0, gridy = 2, insets = ins) {
      add(descriptionLabel, _)
    }

    withConstraints(gridx = 1, gridy = 2, weightx = 1, weighty = 1, fill = Fill.Both, insets = ins) {
      add(new ScrollPane(new Component {
        override lazy val peer = description.peer
      }) {
        horizontalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
        border = EmptyBorder(0)
      }, _)
    }

  }

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 0.4, weightx = 1.0, gridwidth = REMAINDER, anchor = Anchor.SouthWest, insets = ins) {
    add(problemPanel, _)
  }

}

object ProblemsModel {
  val ProblemDataColumn = 3

  def apply(problems: List[Problem]) =
    new ProblemsModel(problems.toArray.map(toArray(_)))

  private def toArray(problem: Problem): Array[AnyRef] =
    Array(problem.status, getReferredMember(problem), problem.description, problem)

  def getReferredMember(p: Problem): String = p match {
    case MissingFieldProblem(oldfld)                   => oldfld.fullName
    case MissingMethodProblem(oldmth)                  => oldmth.fullName
    case MissingClassProblem(oldclz)                   => oldclz.toString
    case MissingPackageProblem(oldpkg)                 => oldpkg.toString
    case InaccessibleFieldProblem(newfld)              => newfld.fullName
    case InaccessibleMethodProblem(newmth)             => newmth.fullName
    case InaccessibleClassProblem(newcls)              => newcls.toString
    case IncompatibleFieldTypeProblem(oldfld, newfld)  => oldfld.fullName
    case IncompatibleMethTypeProblem(oldmth, newmth)   => oldmth.fullName
    case IncompatibleResultTypeProblem(oldmth, newmth) => oldmth.fullName
    case AbstractMethodProblem(oldmeth)                => oldmeth.fullName
  }
}

class ProblemsModel(problems: Array[Array[AnyRef]]) extends AbstractTableModel {
  private val columns = Array("Status", "Member", "Description")
  private val columnsType = Array(classOf[Problem.Status.Value], classOf[String], classOf[String])

  def hasUnfixableProblems = problems.exists(_(0) == Problem.Status.Unfixable)

  override def getColumnName(n: Int) = columns(n)

  override def getColumnCount = columns.size
  override def getRowCount = problems.size
  override def getValueAt(x: Int, y: Int): AnyRef = problems(x)(y)

  override def isCellEditable(i: Int, j: Int) = false

  override def getColumnClass(col: Int) = columnsType(col)

  override def setValueAt(value: AnyRef, row: Int, col: Int): Unit = {
    assert(isCellEditable(row, col))

    problems(row)(col) = value
    fireTableCellUpdated(row, col)
  }
}
