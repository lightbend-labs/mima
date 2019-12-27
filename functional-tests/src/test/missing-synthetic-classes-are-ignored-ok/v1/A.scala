abstract class A {
  def foo = List(new A {}, new A {})
}
