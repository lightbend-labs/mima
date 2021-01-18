package foo

class Foo {
  private[foo] def bar(x: Int, y: Int) = x + y
               def bar(x: String)      = x
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1, 0)
}
