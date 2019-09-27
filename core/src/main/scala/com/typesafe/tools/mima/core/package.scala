package com.typesafe.tools.mima

import java.io.File

import scala.reflect.io.{ AbstractFile, Path }
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath

package object core {
  type ProblemFilter = Problem => Boolean

  private[mima] def pathToClassPath(p: Path): Option[ClassPath] =
    Option(AbstractFile.getDirectory(p)).map(DeprecatedPathApis.newClassPath(_, Config.settings))

  private[mima] def aggregateClassPath(cp: Seq[File]): ClassPath =
    AggregateClassPath.createAggregate(cp.flatMap(pathToClassPath(_)): _*)

  private[core] type Fields  = Members[FieldInfo]
  private[core] type Methods = Members[MethodInfo]
}
