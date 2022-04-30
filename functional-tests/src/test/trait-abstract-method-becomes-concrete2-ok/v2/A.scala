trait A1 {
  def foo: Int = 2
}
trait A extends A1 {
  def bar = 2
}

class B extends A {
  override def foo = 2
}
