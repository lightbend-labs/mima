class A {
  private var a = 5
  def foo(): Unit = {
    val f1 = () => a
    val f2 = (x: Int) => (x * a).toString
    val f3 = (x: Float) => x * a.toFloat
  }
}
