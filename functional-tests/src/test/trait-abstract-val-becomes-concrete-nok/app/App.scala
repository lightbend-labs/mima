object App {
  def main(args: Array[String]): Unit = {
    println(new A { val foo = 11 }.foo + 12)
    println(new B { override val foo = 13 }.foo + 14)
  }
}
