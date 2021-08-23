package pkg1
package pkg2

import scala.annotation.experimental2

trait Quotes {
  val reflect: reflectModule
  trait reflectModule {
    val report: reportModule
    trait reportModule {
      @experimental2
      def errorAndAbort(msg: String): Nothing
    }
  }
}
