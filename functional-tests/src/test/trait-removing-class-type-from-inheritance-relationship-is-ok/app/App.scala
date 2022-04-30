object App {
  def main(args: Array[String]): Unit = {
    val a     = new A {}
    val ab: B = new A {}
    val b     = new B
    val c     = new C
    val cb: B = new C
    val ca: A = new C
    println(a)
    println(ab)
    println(b)
    println(c)
    println(cb)
    println(ca)
  }
}
