object App {
  def main(args: Array[String]): Unit = {
    val foo: Int = new A {}.foo
    println(foo)
  }
}
