trait A {
  def bar = 2
}

trait B {
  def foo = 2
}

class C extends A with B
