abstract class A extends B {
  def foo(a: A): Unit = a.foo()
}

trait B {
  def foo(): Unit
}
