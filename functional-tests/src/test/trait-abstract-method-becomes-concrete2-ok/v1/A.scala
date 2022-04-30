trait A1

trait A extends A1 {
  def bar = 2
  def foo: Int
}

class B extends A {
  override def foo = 2
}
