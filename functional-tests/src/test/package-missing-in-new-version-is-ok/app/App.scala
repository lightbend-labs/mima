object App {
  def main(args: Array[String]): Unit =
    println(new me.A { def foo = 2 }.foo)
}
