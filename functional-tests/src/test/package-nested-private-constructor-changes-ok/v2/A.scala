package foo

object Foo {
  class Bar private[foo](a: Int, val b: String) {}

  object Bar {
    def apply() = new Bar(123, "b")
    def apply(a: String) = new Bar(a.toInt, "b")
  }
}