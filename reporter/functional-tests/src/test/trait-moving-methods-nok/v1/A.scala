trait A {
  def bar = 2
  def foo = 2
}

trait B

class C extends A with B
