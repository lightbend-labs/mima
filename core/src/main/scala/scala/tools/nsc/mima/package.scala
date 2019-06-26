package scala.tools.nsc

import scala.tools.nsc.util.ClassPath

/** Some extension methods to allow accessing private[nsc] members of ClassPath. */
package object mima {
  implicit class ClassPathAccessors(val cp: ClassPath) extends AnyVal {
    def classesIn(pkg: String) = cp.classes(pkg)
    def packagesIn(pkg: String) = cp.packages(pkg)
  }
}
