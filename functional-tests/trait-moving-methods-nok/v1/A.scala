trait A {
  def foo = 2
}

trait B

class C extends A with B
