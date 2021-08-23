package mima
package pkg2

import mima.annotation.exclude

@exclude
class Foo {
  def foo = "1"
}

@annotation.exclude
class Bar {
  def foo = "1"
}
