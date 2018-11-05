package p

trait B[-K, T] {
  def foo(x: Any): Boolean
}

trait A[T] extends B[Any, T] {
  def bar[K]: B[K, T] = this
}
