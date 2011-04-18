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

/**
 * Mima problem report page.
 */
class ReportPage extends GridBagPanel with WithConstraints with wizard.WizardAction {
  import Config.info

  val defaultFilterText = "<enter filter>"

  val ins = new Insets(10, 10, 10, 10)
  withConstraints(insets = ins, gridx = 0, gridy = RELATIVE, anchor = Anchor.West) {
    add(new Label("Comparing the two jar files"), _)
  }
  withConstraints(insets = ins, gridx = 0, fill = Fill.Both)(add(new Separator, _))
  val filter = new TextField(defaultFilterText)
  withConstraints(insets = ins, gridx = 0, fill = Fill.Horizontal)(add(filter, _))

  // the problem table
  val table = new JTable() {
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

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1.0, weightx = 1.0, gridwidth = REMAINDER, insets = ins) {
    add(new ScrollPane(new Component {
      override lazy val peer = table
    }), _)
  }

  var sorter: TableRowSorter[ProblemsModel] = _

  def doCompare(oldDir: String, newDir: String, mimalib: MiMaLib) {
    info("old: " + oldDir + " new: " + newDir)
    val problems = mimalib.collectProblems(oldDir, newDir)
    val model = new ProblemsModel(problems)
    table.setModel(model)
    table.getColumnModel.getColumn(0).setPreferredWidth(70)
    table.getColumnModel.getColumn(1).setPreferredWidth(200)
    table.getColumnModel.getColumn(2).setPreferredWidth(200)
    table.getColumnModel.getColumn(3).setPreferredWidth(40)
    sorter = new TableRowSorter(model)
    table.setRowSorter(sorter)
  }

  // filtering
  listenTo(filter)

  reactions += {
    case ValueChanged(`filter`) =>
      try {
        val rf: RowFilter[ProblemsModel, Integer] = RowFilter.regexFilter(escape(filter.text), 1, 2)
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
      case c => c.toString
    }
  }
}

class ProblemsModel(problems: List[Problem]) extends AbstractTableModel {
  val columns = Array("Status", "Member", "Description", "Fix?")

  import scala.collection.JavaConversions._
  import java.util.Vector

  //  dataVector = new Vector(problems.map(p => new Vector(List(p.status, getReferredMember(p), p.description, false))))

  override def getColumnName(n: Int) = columns(n)

  override def getColumnCount = columns.size
  override def getRowCount = problems.size
  override def getValueAt(x: Int, y: Int) = {
    val p = problems(x)
    y match {
      case 0 => p.status
      case 1 => getReferredMember(p)
      case 2 => p.description
      case 3 => false.asInstanceOf[AnyRef]
    }
  }

  override def isCellEditable(i: Int, j: Int) = j == 3

  //  override def getColumnClass(n: Int) = n match {
  //    case 3 => classOf[Boolean]
  //    case _ => classOf[String]
  //  }

  private def getReferredMember(p: Problem): String = p match {
    case MissingFieldProblem(oldfld) => oldfld.fullName
    case MissingMethodProblem(oldmth) => oldmth.fullName
    case MissingClassProblem(oldclz) => oldclz.toString
    case MissingPackageProblem(oldpkg) => oldpkg.toString
    case InaccessibleFieldProblem(newfld) => newfld.fullName
    case InaccessibleMethodProblem(newmth) => newmth.fullName
    case InaccessibleClassProblem(newcls) => newcls.toString
    case IncompatibleFieldTypeProblem(oldfld, newfld) => oldfld.fullName
    case IncompatibleMethTypeProblem(oldmth, newmth) => oldmth.fullName
    case IncompatibleResultTypeProblem(oldmth, newmth) => oldmth.fullName
    case AbstractMethodProblem(oldmeth) => oldmeth.fullName
  }
}