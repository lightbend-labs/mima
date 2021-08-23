package mima
package pkg2

trait Quotes {
  val reflect: reflectModule
  trait reflectModule {
    val report: reportModule
    trait reportModule
  }
}
