object App {
  def main(args: Array[String]): Unit = {
    println(Usage.useA(new A {}) + 10)
    println(Usage.useB(new B {}) + 11)
    println(Usage.useC(new C {}) + 12)
  }
}
