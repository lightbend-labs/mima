package foo

class Foo {
  private[foo] def bar(x: Int)    = x
               def bar(x: String) = x
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1)
}
