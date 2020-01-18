object App {
  def main(args: Array[String]): Unit = {
    val a = new A
    a.foo = 3
    println(a.foo + 1)
  }
}
