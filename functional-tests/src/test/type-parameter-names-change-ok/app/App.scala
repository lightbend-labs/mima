object App {
  def main(args: Array[String]): Unit = {
    val x: Option[String] = new Tree().someMethod("foo", List(42))
    println(x)
  }
}
