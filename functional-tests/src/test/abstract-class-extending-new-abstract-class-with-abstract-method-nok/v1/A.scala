abstract class A {
  def foo(a: A): Unit = ()
}

class C

class D extends C

abstract class E {
  def bar(): E
}

class F extends E {
  def bar(): E = new F
}
