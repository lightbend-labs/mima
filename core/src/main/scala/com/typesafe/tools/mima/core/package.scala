package com.typesafe.tools.mima

import java.io.File

import scala.reflect.io.AbstractFile
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath

package object core {
  /** Returns `true` for problems to keep, `false` for problems to drop. */
  type ProblemFilter = Problem => Boolean

  private[mima] def pathToClassPath(f: File): Option[ClassPath] =
    Option(AbstractFile.getDirectory(f)).map(DeprecatedPathApis.newClassPath(_, Config.settings))

  private[mima] def aggregateClassPath(cp: Seq[File]): ClassPath =
    AggregateClassPath.createAggregate(cp.flatMap(pathToClassPath(_)): _*)
}
