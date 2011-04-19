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

  private class CheckBoxHeader(itemListener: ItemListener) extends JCheckBox
    with TableCellRenderer with MouseListener {
    protected var column: Int = _
    protected var mousePressed = false

    addItemListener(itemListener)

    override def getTableCellRendererComponent(table: JTable, value: AnyRef,
      isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int) = {
      if (table != null) {
        val header = table.getTableHeader()
        if (header != null) {
          //setForeground(header.getForeground())
          //setBackground(header.getBackground())
          setFont(header.getFont())
          header.addMouseListener(this)
        }
      }
      setColumn(column)
      setText(table.getColumnName(column))
      setBorder(UIManager.getBorder("TableHeader.cellBorder"))
      this
    }

    protected def setColumn(column: Int) = this.column = column

    protected def getColumn() = column

    protected def handleClickEvent(e: java.awt.event.MouseEvent) {
      if (mousePressed) {
        mousePressed = false
        val header = e.getSource.asInstanceOf[JTableHeader]
        val tableView = header.getTable()
        val columnModel = tableView.getColumnModel()
        val viewColumn = columnModel.getColumnIndexAtX(e.getX())
        val column = tableView.convertColumnIndexToModel(viewColumn)

        if (viewColumn == this.column && e.getClickCount() == 1 && column != -1)
          doClick()
      }
    }

    def mouseClicked(e: java.awt.event.MouseEvent) = {
      handleClickEvent(e)
      e.getSource().asInstanceOf[JTableHeader].repaint()
    }

    def mousePressed(e: java.awt.event.MouseEvent) = mousePressed = true

    def mouseReleased(e: java.awt.event.MouseEvent) = ()

    def mouseEntered(e: java.awt.event.MouseEvent) = ()

    def mouseExited(e: java.awt.event.MouseEvent) = ()
  }

  private class CheckBoxHeaderListener(table: JTable) extends ItemListener {
    import java.awt.event.ItemEvent

    def itemStateChanged(e: ItemEvent) {
      val source = e.getSource
      source match {
        case b: CheckBoxHeader =>
          val checked = e.getStateChange() == ItemEvent.SELECTED
          (0 until table.getRowCount).filter(table.getModel.isCellEditable(_, 3)).foreach(table.setValueAt(checked, _, 3))
        case _ => false
      }
    }
  }

  protected class ReportTable extends JTable with javax.swing.event.TableModelListener {
    object LabelCellRenderer extends DefaultTableCellRenderer

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

    override def getCellRenderer(row: Int, column: Int): TableCellRenderer = {
      if (getModel.getValueAt(row, column).isInstanceOf[Boolean] && !getModel.isCellEditable(row, column))
        LabelCellRenderer
      else
        super.getCellRenderer(row, column)
    }
  }

  import Config.info

  private val errorLabel = new Label("Unfortunately there are some unfixable incompatibilities.") {
    foreground = java.awt.Color.RED
  }

  private val defaultFilterText = "<enter filter>"

  private val ins = new Insets(0, 10, 10, 10)

  withConstraints(insets = ins, gridx = 0, gridy = RELATIVE, anchor = Anchor.West) {
    add(errorLabel, _)
  }
  withConstraints(insets = ins, gridx = 0, fill = Fill.Both)(add(new Separator, _))
  private val filter = new TextField(defaultFilterText)
  withConstraints(insets = ins, gridx = 0, fill = Fill.Horizontal)(add(filter, _))

  // the problem table
  private val table = new ReportTable()

  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1.0, weightx = 1.0, gridwidth = REMAINDER, insets = ins) {
    add(new ScrollPane(new Component {
      override lazy val peer = table
    }), _)
  }

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def doCompare(oldDir: String, newDir: String, mimalib: MiMaLib) {
    info("old: " + oldDir + " new: " + newDir)
    val problems = mimalib.collectProblems(oldDir, newDir)
    val model = ProblemsModel(problems)
    sorter = new TableRowSorter(model)
    
    Swing.onEDT {
      errorLabel.visible = model.hasUnfixableProblems
      table.setModel(model)
      table.getColumnModel.getColumn(0).setPreferredWidth(50)
      table.getColumnModel.getColumn(1).setPreferredWidth(100)
      table.getColumnModel.getColumn(2).setPreferredWidth(100)
      table.getColumnModel.getColumn(3).setPreferredWidth(40)
  
      val tc = table.getColumnModel.getColumn(3)
      tc.setHeaderRenderer(new CheckBoxHeader(new CheckBoxHeaderListener(table)))
  
      table.setRowSorter(sorter)
    }
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

  class ProblemsModel(problems: Array[Array[AnyRef]]) extends AbstractTableModel {
    private val columns = Array("Status", "Member", "Description", "Fix?")
    private val columnsType = Array(classOf[String], classOf[String], classOf[String], classOf[java.lang.Boolean], classOf[Problem])

    import scala.collection.JavaConversions._
    import java.util.Vector

    def hasUnfixableProblems = problems.exists(_(0) == Problem.Status.Unfixable)

    def isFixableProblem(row: Array[AnyRef]) = row(0) != Problem.Status.Unfixable

    override def getColumnName(n: Int) = columns(n)

    override def getColumnCount = columns.size
    override def getRowCount = problems.size
    override def getValueAt(x: Int, y: Int): AnyRef = problems(x)(y)

    override def isCellEditable(i: Int, j: Int) =
      j == 3 && isFixableProblem(problems(i))

    override def getColumnClass(col: Int) = columnsType(col)

    override def setValueAt(value: AnyRef, row: Int, col: Int) = {
      problems(row)(col) = value
      fireTableCellUpdated(row, col)
    }
  }
}

