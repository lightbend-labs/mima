package ssol.tools.mima.core

import scala.tools.nsc.util.{JavaClassPath, ClassPath}

class PathResolver(settings: Settings) extends scala.tools.util.PathResolver(settings, DefaultJavaContext) {
  lazy val mimaResult = new JavaClassPath(containers.toIndexedSeq, DefaultJavaContext)
}

object DefaultJavaContext extends ClassPath.JavaContext


