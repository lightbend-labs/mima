package foo

case class Foo private (a: String, b: String) {}
object Foo {
  def apply() = new Foo("a", "b")
  def apply(a: String) = new Foo(a, "b")

  private def unapply(f: Foo) = f
}
