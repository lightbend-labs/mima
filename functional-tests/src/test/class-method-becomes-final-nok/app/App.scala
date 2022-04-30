object App {
  def main(args: Array[String]): Unit =
    println(new A { override def foo = 3 }.foo)
}
