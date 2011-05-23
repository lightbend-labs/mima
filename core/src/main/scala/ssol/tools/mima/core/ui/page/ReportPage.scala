package ssol.tools.mima.core.ui.page

import scala.swing._
import Swing._
import GridBagPanel._

import ssol.tools.mima.core.ui.model.ReportTableModel
import ssol.tools.mima.core.ui.widget.{ReportTable, PopupTableComponent}

import ssol.tools.mima.core.ui.WithConstraints
import ssol.tools.mima._
import ssol.tools.mima.core.Problem
import ssol.tools.mima.core.ui.widget.CloseButton
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
  import javax.swing.event.{ListSelectionListener,ListSelectionEvent} 

  /** Show problem description panel when the user selects a row. */
  private[ReportPage] class RowSelection(table: JTable) extends ListSelectionListener {
    private val LowPanelSplitterHeight = 150
    
    override def valueChanged(e: ListSelectionEvent) {
      var index = table.getSelectedRow

      if (e.getValueIsAdjusting) return // skip

      if (index >= 0) {
        val modelRow = table.convertRowIndexToModel(index)
        problemPanel.problem_=(table.getModel.getValueAt(modelRow, ReportTableModel.ProblemDataColumn).asInstanceOf[Problem])
      }

      problemPanel.visible = index >= 0

      // if we don't force the container to redraw the problem panel won't show up.
      revalidate()

      if (problemPanel.visible) {
        // always set scroll at the top. Delaying call because it has to happen after the container has run `revalidate`
        Swing onEDT {
          val origin = (0,0)
          problemPanel.peer.getViewport.setViewPosition(origin)
          splitPanel.dividerLocation = splitPanel.size.height - LowPanelSplitterHeight
        }
      }
    }
  }

  private val ins = new Insets(0, 0, 10, 0)

  private val defaultFilterText = "<enter filter>"
  private val filter = new TextField(defaultFilterText)

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

  withConstraints(insets = ins, gridx = 0, gridy = 0, weightx = .4, fill = Fill.Both)(add(filter, _))

  private val errorLabel = new Label("Unfortunately there are unfixable incompatibilities") {
    foreground = java.awt.Color.RED
  }
  withConstraints(gridx = 2, gridy = 0, anchor = Anchor.North, insets = new Insets(4, 10, 0, 0)) {
    add(errorLabel, _)
  }

  // the problem table
  private val table =  {
    val t = new ReportTable
    val selectionListener = new RowSelection(t)
    t.getSelectionModel.addListSelectionListener(selectionListener)
    t
  }
  

  val tablePane = new ScrollPane(new Component {
      override lazy val peer = table
    })

  private var sorter: TableRowSorter[AbstractTableModel] = _

  def setTableModel(_model: ReportTableModel) = {
    errorLabel.visible = _model.hasUnfixableProblems

    table.setModel(_model)
    table.doLayout

    sorter = new TableRowSorter(_model)
    table.setRowSorter(sorter)
  }

  private val problemPanel = {
    val panel = new GridBagPanel with WithConstraints {
      private var lightYellow = new Color(247, 255, 199)
      private val backgroundColor = lightYellow
      background = backgroundColor
      border = EmptyBorder(3)

      val closeButton = new CloseButton

      val statusLabel = new Label("Status:")
      val status = new Label

      val fileLabel = new Label("File:")
      val file = new Label

      val memberLabel = new Label("Member:")
      val member = new Label

      val descriptionLabel = new Label("Description:")
      var description = new TextArea {
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
        add(fileLabel, _)
      }

      withConstraints(gridx = 1, gridy = 1, weightx = 1, insets = rightIns) {
        add(file, _)
      }

      withConstraints(gridx = 0, gridy = 2, insets = leftIns) {
        add(memberLabel, _)
      }

      withConstraints(gridx = 1, gridy = 2, weightx = 1, insets = rightIns) {
        add(member, _)
      }

      withConstraints(gridx = 0, gridy = 3, insets = leftIns) {
        add(descriptionLabel, _)
      }

      withConstraints(gridx = 1, gridy = 3, fill = Fill.Horizontal, insets = rightIns) {
        add(description, _)
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
        panel.file.text = problem.fileName
        panel.member.text = problem.referredMember
        panel.description.text = problem.description
      }

      listenTo(panel.closeButton)
      reactions += {
        case ButtonClicked(panel.`closeButton`) => 
          table.clearSelection()
      }
    }
  }
  
  
  
  val splitPanel = new SplitPane(Orientation.Horizontal, tablePane, problemPanel) 
  
  withConstraints(gridx = 0, fill = Fill.Both, weighty = 1, weightx = 1.0, gridwidth = REMAINDER) {
    add(splitPanel, _)
  }

}