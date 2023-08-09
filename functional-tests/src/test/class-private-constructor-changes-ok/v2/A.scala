package foo

class Foo private (val a: String, val b: String) {}
object Foo {
  def apply() = new Foo("a", "b")
  def apply(a: String) = new Foo(a, "b")
}
