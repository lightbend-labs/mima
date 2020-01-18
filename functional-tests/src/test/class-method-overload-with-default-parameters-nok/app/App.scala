object App {
  def main(args: Array[String]): Unit = {
    println(new A().copy())
    println(new A().copy(2))
    println(new A().copy(2, "baz"))
    println(new A().copy(x = 2))
    println(new A().copy(y = "baz"))
    println(new A().copy(x = 2, y = "baz"))
    println(new A().copy(y = "baz", x = 2))
  }
}
