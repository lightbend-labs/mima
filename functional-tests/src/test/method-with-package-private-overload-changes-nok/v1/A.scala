package foo

class Foo {
  private[foo] def bar(x: String) = x
  def bar(x: Int)                 = x
}
