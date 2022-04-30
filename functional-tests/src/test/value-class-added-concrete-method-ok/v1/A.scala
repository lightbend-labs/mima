object A {
  implicit class B(val s: String) extends scala.AnyVal {
    def c(index: Int): Unit = ()
  }
}
