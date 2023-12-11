package foo

class Foo private[foo](a: String) {
  private[foo] def this() = { this("a") }
}

object Foo {
  def apply() = new Foo("a")
}
