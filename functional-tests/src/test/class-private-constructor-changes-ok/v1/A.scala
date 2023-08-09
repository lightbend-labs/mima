package foo

class Foo private (val a: String)

object Foo {
  def apply() = new Foo("a")
}
