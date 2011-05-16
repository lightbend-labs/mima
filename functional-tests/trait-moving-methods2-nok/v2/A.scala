trait A extends B {
  val bar = 2
}

class B {
  def foo: Int = 2
}

class C extends A
