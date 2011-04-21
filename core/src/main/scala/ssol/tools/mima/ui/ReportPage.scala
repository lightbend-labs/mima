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
  protected class ReportTable extends JTable with TableModelListener {
    object UnavailableFixCellRenderer extends TableCellRenderer {
      private val label = new Label("-")
      
      override def getTableCellRendererComponent(table: JTable , value: AnyRef, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): java.awt.Component 	= {
        return label.peer  
      } 
    }
    
    override def prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): java.awt.Component = {
      val component = super.prepareRenderer(renderer, row, column)
      
      component match {
        case jc: javax.swing.JComponent =>
          /** Display a tooltip*/
          if(isUnavailableFixCell(row, column))
            jc.setToolTipText("no fix exists for this issue")
          else if(column != 3) // column == 3 --> don't show tooltip on checkbox
          	jc.setToolTipText(getValueAt(row, column).toString.grouped(80).mkString("<html>", "<br>", "</html>"))
        case _ => ()
      }
      
      component
    }

    override def getCellRenderer(row: Int, column: Int): TableCellRenderer = {
      if (isUnavailableFixCell(row, column))
        UnavailableFixCellRenderer
      else
        super.getCellRenderer(row, column)
    }
    
    private def isUnavailableFixCell(row: Int, column: Int): Boolean = 
      getModel.getValueAt(row, column).isInstanceOf[Boolean] && !getModel.isCellEditable(row, column)
    
  }

  private val errorLabel = new Label("Unfortunately there are some unfixable incompatibilities") {
    foreground = java.awt.Color.RED
  }

  private val ins = new Insets(0, 10, 10, 10)

  withConstraints(insets = ins, gridwidth = 2, gridx = REMAINDER, gridy = REMAINDER, fill = Fill.Horizontal) {
    add(errorLabel, _)
  }
  
  private val defaultFilterText = "<enter filter>"
  private val filter = new TextField(defaultFilterText)
  
  withConstraints(insets = ins, gridx = 0, gridy = 1, weightx = .5, fill = Fill.Both)(add(filter, _))
  
  private val fixAll = new CheckBox()
  
  fixAll.action = new Action("Fix all") {
    override def apply() {
      (0 until table.getRowCount).filter(table.isCellEditable(_, 3)).foreach(table.setValueAt(fixAll.selected, _, 3))
    }
  }
  withConstraints(insets = ins, gridx=1, gridy = 1, weightx = .5)(add(fixAll, _))

  // the problem table
  private val table = new ReportTable

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1.0, weightx = 1.0, gridwidth = REMAINDER, insets = ins) {
    add(new ScrollPane(new Component {
      override lazy val peer = table
    }), _)
  }

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def model: ProblemsModel = table.getModel.asInstanceOf[ProblemsModel]

  def model_=(model: ProblemsModel) = {
    errorLabel.visible = model.hasUnfixableProblems
    
    table.setModel(model)
    table.getColumnModel.getColumn(0).setPreferredWidth(50)
    table.getColumnModel.getColumn(1).setPreferredWidth(100)
    table.getColumnModel.getColumn(2).setPreferredWidth(100)
    table.getColumnModel.getColumn(3).setPreferredWidth(40)

    sorter = new TableRowSorter(model)
    table.setRowSorter(sorter)
  }

  // filtering
  listenTo(filter)

  reactions += {
    case ValueChanged(`filter`) =>
      try {
        val rf: RowFilter[AbstractTableModel, Integer] = RowFilter.regexFilter(escape(filter.text), 1, 2)
        sorter.setRowFilter(rf)
      } catch {
        case _ => () // swallow any illegal regular expressions
      }
    case FocusGained(`filter`, _, _) if (filter.text == defaultFilterText) =>
      filter.text = ""
  }

  private def escape(str: String): String = {
    str flatMap {
      case '$' => "\\$"
      case '+' => "\\+"
      case c   => c.toString
    }
  }
}

object ProblemsModel {
  def apply(problems: List[Problem]) =
    new ProblemsModel(problems.toArray.map(toArray(_)))

  private def toArray(problem: Problem): Array[AnyRef] =
    Array(problem.status, getReferredMember(problem), problem.description, false.asInstanceOf[AnyRef], problem)

  private def getReferredMember(p: Problem): String = p match {
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
  private val columns = Array("Status", "Member", "Description", "Fix?")
  private val columnsType = Array(classOf[String], classOf[String], classOf[String], classOf[java.lang.Boolean])

  import scala.collection.JavaConversions._
  import java.util.Vector

  def hasUnfixableProblems = problems.exists(_(0) == Problem.Status.Unfixable)

  def isFixableProblem(row: Array[AnyRef]) = row(0) == Problem.Status.Fixable

  override def getColumnName(n: Int) = columns(n)

  override def getColumnCount = columns.size
  override def getRowCount = problems.size
  override def getValueAt(x: Int, y: Int): AnyRef = problems(x)(y)

  override def isCellEditable(i: Int, j: Int) =
    j == 3 && isFixableProblem(problems(i))
  
  override def getColumnClass(col: Int) = columnsType(col)

  def selectedProblems: Traversable[Problem] = problems.filter(_(3).asInstanceOf[Boolean]).map(_(4).asInstanceOf[Problem])
  
  override def setValueAt(value: AnyRef, row: Int, col: Int) = {
    problems(row)(col) = value
    fireTableCellUpdated(row, col)
  }
}
