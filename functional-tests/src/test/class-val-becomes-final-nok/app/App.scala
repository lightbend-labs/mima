object App {
  def main(args: Array[String]): Unit =
    println(new A { override val foo = 3 }.foo)
}
