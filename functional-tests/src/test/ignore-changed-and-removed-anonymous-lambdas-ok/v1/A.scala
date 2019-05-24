class A {
  private var a = 5
  def foo(): Unit = {
    val f1 = () => a.toString
    val f2 = (x: Int) => x * a
    val f3 = (x: Float) => x * a.toFloat
  }
}
