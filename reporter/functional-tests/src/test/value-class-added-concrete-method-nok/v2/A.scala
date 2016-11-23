object A {
  implicit class B(val s: String) extends scala.AnyVal {
    def c(index: Int  ): Unit = ()
    def c(range: Range): Unit = ()
  }
}
