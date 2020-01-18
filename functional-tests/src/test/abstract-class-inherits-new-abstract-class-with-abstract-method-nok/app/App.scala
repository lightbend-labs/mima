object App {
  def main(args: Array[String]): Unit = {
    println(Usage.use(new A {}))
    println(Usage.use(new B {}))
  }
}
