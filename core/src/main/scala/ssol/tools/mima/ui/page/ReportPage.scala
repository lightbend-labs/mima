package ssol.tools.mima.ui.page

import scala.swing._
import Swing._
import GridBagPanel._

import ssol.tools.mima.ui.{ WithConstraints, FixHint }
import ssol.tools.mima._
import ssol.tools.mima.ui.widget.CloseButton
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

      // if we don't force the container to redraw the problem panel won't show up.
      revalidate()

      if (problemPanel.visible) {
        // always set scroll the top. Delaying call because it has to happen after the container has run `revalidate`
        Swing onEDT {
          problemPanel.peer.getViewport.setViewPosition((0, 0))
        }
      }
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

  private val ins = new Insets(0, 0, 10, 0)

  private val defaultFilterText = "<enter filter>"
  private val filter = new TextField(defaultFilterText)

  withConstraints(insets = ins, gridx = 0, gridy = 0, weightx = .4, fill = Fill.Both)(add(filter, _))

  private val errorLabel = new Label("Unfortunately there are unfixable incompatibilities") {
    foreground = java.awt.Color.RED
  }
  withConstraints(gridx = 2, gridy = 0, anchor = Anchor.North, insets = new Insets(4, 10, 0, 0)) {
    add(errorLabel, _)
  }

  // the problem table
  private val table = new ReportTable
  val selectionListener = new RowSelection(table)
  table.getSelectionModel.addListSelectionListener(selectionListener)

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 0.6, weightx = 1.0, gridwidth = REMAINDER) {
    add(new ScrollPane(new Component {
      override lazy val peer = table
    }), _)
  }

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def setTableModel(_model: ProblemsModel) = {
    errorLabel.visible = _model.hasUnfixableProblems

    table.setModel(_model)

    sorter = new TableRowSorter(_model)
    table.setRowSorter(sorter)
  }

  // filtering
  listenTo(filter)

  reactions += {
    case ValueChanged(`filter`) =>
      try {
        val rf: RowFilter[AbstractTableModel, Integer] = if (filter.text != defaultFilterText) RowFilter.regexFilter(escape(filter.text), 1) else RowFilter.regexFilter("*")
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
  private val problemPanel = {
    val panel = new GridBagPanel with WithConstraints {
      private val backgroundColor = new Color(247, 255, 199) // light-yellow
      background = backgroundColor
      border = EmptyBorder(3)

      val closeButton = new CloseButton

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

      val fixHintLabel = new Label("Fix Hint:")
      var fixHint = new TextArea {
        editable = false
        background = backgroundColor
        lineWrap = true
        charWrap = true
      }

      val leftIns = new Insets(0, 9, 10, 5)
      val rightIns = new Insets(0, 0, 10, 9)

      withConstraints(gridwidth = 2, anchor = Anchor.FirstLineEnd, insets = new Insets(0, 0, 0, 12)) {
        add(closeButton, _)
      }

      withConstraints(gridx = 0, gridy = 0, insets = leftIns) {
        add(statusLabel, _)
      }

      withConstraints(gridx = 1, gridy = 0, weightx = 1, insets = rightIns) {
        add(status, _)
      }

      withConstraints(gridx = 0, gridy = 1, insets = leftIns) {
        add(memberLabel, _)
      }

      withConstraints(gridx = 1, gridy = 1, weightx = 1, insets = rightIns) {
        add(member, _)
      }

      withConstraints(gridx = 0, gridy = 2, insets = leftIns) {
        add(descriptionLabel, _)
      }

      withConstraints(gridx = 1, gridy = 2, fill = Fill.Horizontal, insets = rightIns) {
        add(description, _)
      }

      withConstraints(gridx = 0, gridy = 3, insets = new Insets(0, 9, 0, 5)) {
        add(fixHintLabel, _)
      }

      withConstraints(gridx = 1, gridy = 3, weightx = 1, fill = Fill.Horizontal, insets = new Insets(0, 0, 0, 9)) {
        add(fixHint, _)
      }

      withConstraints(gridx = 0, gridy = 4, gridwidth = 2, weightx = 1, weighty = 1, fill = Fill.Both) {
        add(Swing.VGlue, _)
      }
    }

    new ScrollPane {
      private val view = new Component {
        override lazy val peer = panel.peer
      }

      contents = view

      visible = false

      import javax.swing.ScrollPaneConstants._
      horizontalScrollBarPolicy = new ScrollPane.BarPolicy.Value(HORIZONTAL_SCROLLBAR_NEVER, VERTICAL_SCROLLBAR_AS_NEEDED)
      border = LineBorder(Color.lightGray, 1)

      private var _problem: Problem = _
      def problem_=(problem: Problem) = {
        panel.status.text = problem.status.toString
        panel.member.text = ProblemsModel.getReferredMember(problem)
        panel.description.text = problem.description
        FixHint(problem) match {
          case Some(hint) =>
            panel.fixHint.text = "To fix this incompatibility consider adding the following bridge " +
              "method in the class source code:\n\n" + hint.toSourceCode
            showFixPanel(true)
          case None =>
            showFixPanel(false)
        }
      }

      listenTo(panel.closeButton)
      reactions += {
        case ButtonClicked(panel.`closeButton`) => {
          table.clearSelection()
        }
      }

      private def showFixPanel(show: Boolean) = {
        panel.fixHintLabel.visible = show
        panel.fixHint.visible = show
      }
    }
  }

  withConstraints(gridx = 0, fill = Fill.Both, weightx = 1.0, weighty = .4, gridwidth = REMAINDER, anchor = Anchor.SouthWest) {
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
    case ClassAndTraitNotComparableProblem(oldclz, _)  => oldclz.fullName
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
