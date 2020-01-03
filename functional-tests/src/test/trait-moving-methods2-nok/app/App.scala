object App {
  def main(args: Array[String]): Unit = {
    println(new A {}.foo + 10)
    println(new A {}.bar + 11)
  }
}
