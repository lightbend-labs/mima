object App {
  def main(args: Array[String]): Unit = {
    println(new A().foo)
    println(new A { override val foo = 1 }.foo)
  }
}
