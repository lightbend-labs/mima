abstract class A extends B {
  def foo(a: A): Unit = a.foo()
}

abstract class B {
  def foo(): Unit
}

class C extends B {
  def foo(): Unit = ()
}

class D extends C


abstract class AA {
  def bar(): AA
}

abstract class E extends AA {
  def bar(): this.type
}

class F extends E {
  def bar(): this.type = this
}
