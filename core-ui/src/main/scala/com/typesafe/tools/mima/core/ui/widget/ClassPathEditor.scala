package com.typesafe.tools.mima.core.ui.widget

import scala.collection.mutable

import scala.tools.nsc.util.ClassPath

import scala.swing._
import event._
import Swing._
import BorderPanel._

import com.typesafe.tools.mima.core.ui.images
import com.typesafe.tools.mima.core.ui.WithConstraints

/** A simple interface for interacting with the classpath. Allows
 *  one to reorder and add/remove entries using a list view.
 */
class ClassPathEditor(init: List[String] = Nil) extends GridBagPanel with WithConstraints {
  private var elements = mutable.ListBuffer(init: _*)

  import java.awt.Color
  import java.awt.GridBagConstraints

  protected val classpathLabel = new Label("Classpath:")

  def classpath_=(_classpath: List[String]) =  listView.listData = _classpath
  def classpath = listView.listData
  
  /** Return the string of current classpath in this editor. */
  def classPathString = {
    ClassPath.join(listView.listData: _*)
  }
  
  private val listView = new ListView(elements.toList) {
    border = LineBorder(Color.gray)
    selection.intervalMode = ListView.IntervalMode.SingleInterval

    listenTo(mouse.moves)
    
    reactions += {
      case MouseMoved(_, point, _) =>
        val index = peer.locationToIndex(point)
        if (index > -1)
          tooltip = listData(index)
        else
          tooltip = null
    }
  }

  private val scrollableList = new ScrollPane {
    contents = listView
  }

  val addEntry = new Button("") {
    icon = images.Icons.add
    horizontalAlignment = Alignment.Left
  }
  val removeEntry = new Button("") {
    icon = images.Icons.remove
    horizontalAlignment = Alignment.Left
  }
  val moveUp = new Button("") {
    icon = images.Icons.up
    horizontalAlignment = Alignment.Left
  }
  val moveDown = new Button("") {
    icon = images.Icons.down
    horizontalAlignment = Alignment.Left
  }

  import GridBagPanel._
  import GridBagConstraints._

  withConstraints(gridwidth = 2, insets = new Insets(0, 0, 10, 0))(add(classpathLabel, _))

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
          listView.listData ++= d.selectedFiles map (_.getAbsolutePath)
        case _ =>
      }

    case ButtonClicked(`moveUp`) =>
      val sel = listView.selection
      val indices = sel.indices.toList
      val (prefix, suffix) = listView.listData.splitAt(indices.min)
      listView.listData = prefix.dropRight(1) ++ sel.items ++ List(prefix.last) ++ suffix.drop(indices.size)
      listView.selectIndices((indices map (_ - 1)).toSeq: _*)

    case ButtonClicked(`moveDown`) =>
      val sel = listView.selection
      val indices = sel.indices.toList
      val (prefix, suffix) = listView.listData.splitAt(sel.indices.max + 1)
      listView.listData = prefix.dropRight(sel.indices.size) ++ List(suffix.first) ++ sel.items ++ suffix.drop(1)
      listView.selectIndices((indices map (_ + 1)).toSeq: _*)
  }

  /** Enable/disable buttons according to the list selection. */
  private def updateButtonStatus() {
    moveUp.enabled = listView.selection.items.nonEmpty && !(listView.selection.indices contains 0)
    moveDown.enabled = listView.selection.items.nonEmpty && !(listView.selection.indices contains (listView.listData.length - 1))
    removeEntry.enabled = listView.selection.items.nonEmpty
  }
}
