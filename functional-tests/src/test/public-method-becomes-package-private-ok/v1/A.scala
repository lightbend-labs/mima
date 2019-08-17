package bar
class A[T](t: T)
object A {
  def foo[T](implicit x: T): A[T] = ???
  def foo[T](implicit x: T, y: T): A[T] = ???
  def foo[T](f: T => T): A[T] = ???
  def foo[T](f: (T, T) => T): A[T] = ???
}
