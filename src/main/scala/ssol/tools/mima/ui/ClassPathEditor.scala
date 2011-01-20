package ssol.tools.mima.ui

import scala.collection.mutable

import scala.tools.nsc.util.ClassPath

import scala.swing._
import event._
import Swing._
import BorderPanel._

/** A simple interface for interacting with the classpath. Allows
 *  one to reorder and add/remove entries using a list view.
 */
class ClassPathEditor(init: List[String]) extends GridBagPanel {
  private var elements = mutable.ListBuffer(init: _*)

  import java.awt.Color
  import java.awt.GridBagConstraints
  
  private val listView = new ListView(elements.toList) {
    border = LineBorder(Color.gray)
    selection.intervalMode = ListView.IntervalMode.SingleInterval
  }
  
  private val scrollableList = new ScrollPane {
    contents = listView
  }
  
  /** Load the icon from the given path, or EmptyIcon if not found. */
  private def getIcon(path: String): javax.swing.Icon = {
    val resource = Swing.Icon(getClass.getResource(path))
    if (resource eq null) EmptyIcon else resource
  }
  
  val addEntry = new Button("Add Entry") {
    icon = getIcon("/images/add.png")
    horizontalAlignment = Alignment.Left
  }
  val removeEntry = new Button("Remove Entry") {
    icon = getIcon("/images/remove.gif")
    horizontalAlignment = Alignment.Left
  }
  val moveUp      = new Button("Up") {
    icon = getIcon("/images/up.gif")
    horizontalAlignment = Alignment.Left
  }
  val moveDown    = new Button("Down") {
    icon = getIcon("/images/down.gif")
    horizontalAlignment = Alignment.Left
  }
  
  import GridBagPanel._
  import GridBagConstraints._
  
  /** Convenience method for creating and adding components to a GridBagPanel.
   *  It has reasonable defaults for all parameters.
   */
  def withConstraints[T](gridx: Int = RELATIVE, 
      gridy: Int = RELATIVE, 
      gridwidth: Int = 1,
      gridheight: Int = 1, 
      weightx: Double = 0.0,
      weighty: Double = 0.0, 
      anchor: Anchor.Value = Anchor.NorthWest,
      fill: Fill.Value = Fill.None,
      insets: Insets = new Insets(0, 0, 0, 0), 
      ipadx: Int = 0,
      ipady: Int = 0)(op: Constraints => T) = {
        val c = new Constraints(gridx, gridy, 
                                  gridwidth, gridheight, 
                                  weightx, weighty, 
                                  anchor.id, fill.id, insets, 
                                  ipadx, ipady)
        op(c)
  }
  
//    border = LineBorder(Color.RED)

  withConstraints(gridwidth = 2)(add(new Label("Classpath"), _))
  withConstraints(gridx = 0, gridy = 1, fill = Fill.Both, weightx = 1.0, weighty = 1.0, gridheight = REMAINDER)(add(scrollableList, _))
  
  // add buttons
  withConstraints(gridx = 1, anchor = Anchor.NorthWest, fill = Fill.Horizontal) { c =>
    add(addEntry, c)
    add(removeEntry, c)
    add(moveUp, c)
    add(moveDown, c)
  }

  updateButtonStatus()
  listenTo(listView.selection, moveUp, moveDown, addEntry, removeEntry)
  
  reactions += {
    case ListSelectionChanged(`listView`, range, live) =>
      updateButtonStatus()
      
    case ButtonClicked(`removeEntry`) =>
      listView.listData = listView.listData.filterNot(listView.selection.items contains _)
      
    case ButtonClicked(`addEntry`) =>
      import FileChooser._
      
      val d = new ClassPathFileChooser
      d.showOpenDialog(this) match {
        case Result.Approve => 
          listView.listData ++= List(d.selectedFile.getAbsolutePath)
        case _ =>
      }
      
    case ButtonClicked(`moveUp`) =>
      val sel  = listView.selection
      val indices = sel.indices.toList
      val (prefix, suffix) = listView.listData.splitAt(indices.min)
      listView.listData = prefix.dropRight(1) ++ sel.items ++ List(prefix.last) ++ suffix.drop(indices.size)
      listView.selectIndices((indices map (_ - 1)).toSeq: _*)

    case ButtonClicked(`moveDown`) =>
      val sel  = listView.selection
      val indices = sel.indices.toList
      val (prefix, suffix) = listView.listData.splitAt(sel.indices.max + 1)
      listView.listData = prefix.dropRight(sel.indices.size) ++ List(suffix.first) ++ sel.items ++ suffix.drop(1)
      listView.selectIndices((indices map (_ + 1)).toSeq: _*)
  }
  
  /** Return the string of current classpath in this editor. */
  def classPathString = {
    ClassPath.join(listView.listData: _*)
  }

  /** Enable/disable buttons according to the list selection. */
  private def updateButtonStatus() {
    moveUp.enabled = listView.selection.items.nonEmpty && !(listView.selection.indices contains 0)
    moveDown.enabled = listView.selection.items.nonEmpty && !(listView.selection.indices contains (listView.listData.length - 1))
    removeEntry.enabled = listView.selection.items.nonEmpty
  }
}
