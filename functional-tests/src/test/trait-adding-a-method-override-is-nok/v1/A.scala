trait A {
  def m = "a"
  def n = "a"
}

trait B extends A {
  override def m = "b"
}
