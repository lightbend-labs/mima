trait A[T] { def f: T = ??? }
object T extends A[String] { override def f: String = "" }
