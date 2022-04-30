object App {
  def main(args: Array[String]): Unit =
    println(new A { def foo = "bob" }.foo)
}
