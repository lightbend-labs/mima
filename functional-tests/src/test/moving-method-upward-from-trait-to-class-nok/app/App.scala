object App {
  def main(args: Array[String]): Unit = {
    val b: B = new B {}
    println(b.foo + 1)
  }
}
