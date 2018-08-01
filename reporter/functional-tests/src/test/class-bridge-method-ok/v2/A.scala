package scala

class A {
  def foo: Object = 2.asInstanceOf[AnyRef]
  def foo: Int = (foo: Object).asInstanceOf[Int]
}
