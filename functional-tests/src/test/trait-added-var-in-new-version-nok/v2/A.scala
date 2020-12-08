trait A {
  var bar = 3
}

object UseA {
  def useA(a: A): Unit = println(a.bar + 1)
}
