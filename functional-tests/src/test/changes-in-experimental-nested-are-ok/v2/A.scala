package mima
package pkg2

import mima.annotation.exclude

trait Quotes {
  val reflect: reflectModule
  trait reflectModule {
    val report: reportModule
    trait reportModule {
      @exclude
      def errorAndAbort(msg: String): Nothing
    }
  }
}
