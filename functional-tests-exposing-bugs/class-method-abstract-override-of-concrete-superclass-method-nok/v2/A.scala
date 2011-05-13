class A {
  def foo = 2
}

abstract class B extends A {
  override def foo: Int
}