package com.typesafe.tools.mima.core

object TastyRefs {

  /** An address pointing to an index in a TASTy buffer's byte array */
  case class Addr(index: Int) extends AnyVal {
    def -(delta: Int): Addr = Addr(index - delta)
    def +(delta: Int): Addr = Addr(index + delta)

    def relativeTo(base: Addr): Addr = this - base.index - AddrWidth

    def ==(that: Addr): Boolean = index == that.index
    def !=(that: Addr): Boolean = index != that.index

    def <(that: Addr): Boolean = index < that.index
  }

  /**
   * The maximal number of address bytes. Since addresses are written as base-128 natural numbers, the value of 4 gives
   * a maximal array size of 256M.
   */
  final val AddrWidth = 4
}
