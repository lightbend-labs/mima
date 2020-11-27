package foo

class Foo {
  private[foo] val bar: String = "1"
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar.length
}
