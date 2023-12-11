package foo

object Foo {
  class Bar private[foo](a: String) {}

  object Bar {
    def apply() = new Bar("a")
  }
}