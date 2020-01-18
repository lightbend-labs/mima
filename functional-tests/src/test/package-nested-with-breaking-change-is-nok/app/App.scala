object App {
  def main(args: Array[String]): Unit = {
    val bytes: Array[Byte] = Array(1)
    println(new A().toHex(bytes))
    println(new p.A().toHex(bytes))
    println(new p.q.A().toHex(bytes))
    println(new p.q.r.A().toHex(bytes))
    println(new p.q.r.s.A().toHex(bytes))
  }
}
