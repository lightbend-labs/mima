package mima
package pkg2

import mima.annotation.exclude

class Foo {
  @exclude
  def foo = "1"

  @annotation.exclude
  def bar = "1"
}
