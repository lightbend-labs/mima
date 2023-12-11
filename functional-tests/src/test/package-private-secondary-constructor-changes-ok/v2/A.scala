package foo

class Foo private[foo] (val a: String, val b: String) {
  private[foo] def this() = { this("a", "b") }
}
object Foo {
  def apply() = new Foo()
  def apply(a: String) = new Foo(a, "b")
}
