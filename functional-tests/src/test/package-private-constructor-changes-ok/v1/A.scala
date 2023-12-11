package foo

class Foo private[foo](a: String) {}

object Foo {
  def apply() = new Foo("a")
}
