class A {
  def genericWithChangingName[U]: Option[U]     = ???
  def backwardsCompatibleNarrowing: Option[Any] = ???
}

final class Api {
  def cov1(): Any  = ()
  def cov2[T](): T = null.asInstanceOf[T]

  def con1(x: Any): Unit  = ()
  def con2[T](x: T): Unit = ()
}

abstract class Abi {
  def cov1(): Any
  def cov2[T](): T

  def con1(x: Any): Unit
  def con2[T](x: T): Unit
}
