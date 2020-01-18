abstract class A
abstract class B extends A
abstract class C extends B {
  def foo(): Unit
}
abstract class D extends C

object Usage {
  def use(a: A) = ()
}
