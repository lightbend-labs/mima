trait DialogSource[+A] { def show(): A }

final class OptionPane[A](val source: (MyPane[A], String)) extends DialogSource[A] {
  def show(): A = {
    val (pane, _) = source
    pane.result
  }
}

trait MyPane[A] {
  def result: A
}
