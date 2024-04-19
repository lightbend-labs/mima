object App {
  def main(args: Array[String]): Unit = {
    val b = new B {}
    b.foo(1)
    b.foo(-1)
  }
}
