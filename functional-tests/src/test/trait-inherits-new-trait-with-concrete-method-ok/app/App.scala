object App {
  def main(args: Array[String]): Unit = {
    println(new A {})
    println(new B {})
    val ba: A = new B {}
    println(ba)
  }
}
