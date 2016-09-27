trait AA {
  def foo(): Unit
}

trait A extends AA
abstract class B extends A
