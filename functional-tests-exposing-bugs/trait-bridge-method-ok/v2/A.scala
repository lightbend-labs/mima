package scala

import annotation.bridge

trait A {
  def foo: Object = 2.asInstanceOf[AnyRef]
  @bridge def foo: Int = (foo: Object).asInstanceOf[Int]
}