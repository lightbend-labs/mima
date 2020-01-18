object App {
  def main(args: Array[String]): Unit = {
    println(new me.A { def foo = 1 }.foo)
    println(new you.B)
  }
}
