package ssol.tools.mima

import scala.tools.nsc.util.{JavaClassPath, ClassPath}

class PathResolver(settings: Settings) extends scala.tools.util.PathResolver(settings, DefaultJavaContext) {
//  lazy val mimaBasis = Calculated.basis.tail // drop java bootclasspath
//  lazy val mimaContainersVup = basisVup.flatten.distinct

  lazy val mimaResult = new JavaClassPath(containers, DefaultJavaContext)
}

object DefaultJavaContext extends ClassPath.JavaContext


