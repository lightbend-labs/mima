package ssol.tools.mima.lib.ui.model

import ssol.tools.mima.core.Problem
import javax.swing.table.AbstractTableModel

object ReportTableModel {
  private val ProblemDataColumn = 3

  def apply(problems: List[Problem]) =
    new ReportTableModel(problems.toArray.map(toArray(_)))

  private def toArray(problem: Problem): Array[AnyRef] =
    Array(problem.status, problem.referredMember, problem.description, problem)
}

class ReportTableModel(problems: Array[Array[AnyRef]]) extends AbstractTableModel {
  private val columns = Array("Status", "Member", "Description")
  private val columnsType = Array(classOf[Problem.Status.Value], classOf[String], classOf[String])

  def hasUnfixableProblems = problems.exists(_(0) == Problem.Status.Unfixable)

  def countUnfixableProblems = problems.count(_(0) == Problem.Status.Unfixable)
  
  def countUpgradableProblems = problems.count(_(0) == Problem.Status.Upgradable)
  
  override def getColumnName(n: Int) = columns(n)
  def getColumnNames = columns.toList
  def getColumnIndex(name: String) = (0 until columns.size).find(getColumnName(_).toLowerCase == name) 

  override def getColumnCount = columns.size
  override def getRowCount = problems.size
  override def getValueAt(x: Int, y: Int): AnyRef = problems(x)(y)
  
  def getProblem(row: Int) = getValueAt(row, ReportTableModel.ProblemDataColumn).asInstanceOf[Problem]

  override def isCellEditable(i: Int, j: Int) = false

  override def getColumnClass(col: Int) = columnsType(col)

  override def setValueAt(value: AnyRef, row: Int, col: Int): Unit = {
    assert(isCellEditable(row, col))

    problems(row)(col) = value
    fireTableCellUpdated(row, col)
  }
}