trait A {
  def foo: Int
}



trait B extends A {
  override def foo: Int = 2
}

class C extends B