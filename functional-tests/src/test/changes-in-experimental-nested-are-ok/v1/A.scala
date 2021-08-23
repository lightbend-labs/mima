package pkg1
package pkg2

trait Quotes {
  val reflect: reflectModule
  trait reflectModule {
    val report: reportModule
    trait reportModule
  }
}
