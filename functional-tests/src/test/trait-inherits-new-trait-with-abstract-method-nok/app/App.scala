object App {
  def main(args: Array[String]): Unit = {
    println(Usage.useAfoo(new A {}) + 11)
    println(Usage.useAbar(new A {}) + 12)
    println(Usage.useAfoo(new B {}) + 13)
    println(Usage.useAbar(new B {}) + 14)
    println(Usage.useBfoo(new B {}) + 15)
    println(Usage.useBbar(new B {}) + 16)
  }
}
