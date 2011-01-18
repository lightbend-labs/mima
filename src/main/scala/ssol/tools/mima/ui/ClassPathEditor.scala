package ssol.tools.mima.ui

import scala.swing._
import Swing._
import BorderPanel._

class ClassPathEditor extends BorderPanel {
  private var elements: List[String] = List("jar1", "jar2")

  private val listView = new ListView(elements) {
    border = BeveledBorder(Lowered)
//    border = EmptyBorder(10, 10, 10, 10)
  }
  
  private val title = new Label("Classpath") 
  add(title, Position.North)
  
  val bottom = new GridBagPanel {
    import GridBagPanel._
    
    val c = new Constraints
    c.weightx = 1.0
    layout(HStrut(10)) = c
    
    c.fill = Fill.Both
    c.gridy = 1
    c.weighty = 1.0
    layout(listView) = c
  }
  
  add(bottom, Position.Center)
}