object App {
  def main(args: Array[String]): Unit = {
    object a extends A { def baz = 2 }
    println(a.baz)
  }
}
