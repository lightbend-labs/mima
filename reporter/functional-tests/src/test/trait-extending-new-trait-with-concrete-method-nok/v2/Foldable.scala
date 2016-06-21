trait FoldableToList[F[_]] { self: Foldable[F] =>
  def toList[A](fa: F[A]): List[A] =
    self.foldLeft(fa, List.empty[A])((xs, x) => x :: xs).reverse
}

trait Foldable[F[_]] extends FoldableToList[F]{
  def foldLeft[A, B](fa: F[A], z: B)(f: (B, A) => B): B
}
