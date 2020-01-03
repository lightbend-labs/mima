object App {
  def main(args: Array[String]): Unit = {
    val foldable = new Foldable[Option] {
      def foldLeft[A, B](fa: Option[A], z: B)(f: (B, A) => B): B = fa.fold(z)(f(z, _))
    }
    println(foldable.foldLeft(Some("abc"), "=")(_ + _))
    println(foldable.foldLeft(None, "=")(_ + _))
  }
}
