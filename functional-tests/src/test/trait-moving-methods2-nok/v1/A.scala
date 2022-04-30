trait A extends B {
  def foo: Int = 2
  val bar      = 2
}

class B

class C extends A
