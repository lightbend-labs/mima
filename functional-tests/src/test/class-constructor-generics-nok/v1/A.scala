trait DialogSource[+A] { def show(): A }

final class OptionPane(val source: (String, String)) extends DialogSource[String] {
  def show(): String = {
    val (content, _) = source
    content
  }
}
