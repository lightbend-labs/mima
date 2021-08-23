package pkg1
package pkg2

import scala.annotation.experimental2

@experimental2
class Foo {
  def foo = 1
}

@annotation.experimental2
class Bar {
  def foo = 1
}
