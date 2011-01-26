package ssol.tools.mima
package ui

import scala.swing._
import Swing._
import GridBagPanel._

import javax.swing.table._

import java.awt.Color
import java.awt.GridBagConstraints._

/** Mima problem report page.
 */
class ReportPage extends GridBagPanel with WithConstraints {

  val ins = new Insets(10, 10, 10, 10)
  withConstraints(insets = ins, gridx = 0, gridy = RELATIVE, anchor = Anchor.West) { 
    add(new Label("Comparing the two jar files") { border = LineBorder(Color.RED) }, _)
  }
  withConstraints(insets = ins, gridx = 0, fill = Fill.Both)(add(new Separator, _))

  // the problem table
  val table = new Table

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1.0, weightx = 1.0, gridwidth = REMAINDER, insets = ins) {
    add(new ScrollPane(table), _)
  }
  
  def doCompare(oldDir: String, newDir: String, mimalib: MiMaLib) {
    println("old: " + oldDir + " new: " + newDir)
    val problems = mimalib.collectProblems(oldDir, newDir)
    table.model = new ProblemsModel(problems)
  }
}

class ProblemsModel(problems: List[Problem]) extends AbstractTableModel {
  val columns = Array("Status", "Description", "Fix?")
  
  override def getColumnName(n: Int) = columns(n)
  
  def getColumnCount = columns.size
  def getRowCount = problems.size
  def getValueAt(x: Int, y: Int) = { 
    val p = problems(x)
    y match {
      case 0 => p.status
      case 1 => p.description
      case 2 => "fix?"
    }
  }
  
}