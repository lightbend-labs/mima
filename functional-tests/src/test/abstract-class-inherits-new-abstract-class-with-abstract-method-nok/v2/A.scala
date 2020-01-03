abstract class AA {
  def foo(): Unit
}

abstract class A extends AA
abstract class B extends A
abstract class C extends B {
  def foo(): Unit
}
abstract class D extends C

object Usage {
  def use(a: A) = a.foo()
}
