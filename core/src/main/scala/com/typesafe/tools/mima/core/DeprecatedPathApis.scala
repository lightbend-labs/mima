package com.typesafe.tools.mima.core

import scala.tools.nsc
import scala.tools.nsc.classpath.ClassPathFactory
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.ClassPath
import scala.tools.util.PathResolver

// As of Scala 2.12.9 + 2.13.0 these compiler APIs are deprecated
// because they have new overloads that also take a scala.tools.nsc.CloseableRegistry
// In sbt the compiler used is determined by scalaVersion
// So we must continue to use the old APIs to use MiMa on builds with Scala <2.12.9.
// So use the WORKAROUND in scala/bug#7934
private[core] object DeprecatedPathApis extends DeprecatedPathApis

@deprecated("", "")
private[core] class DeprecatedPathApis {
  def newPathResolver(settings: nsc.Settings): PathResolver              = new PathResolver(settings)
  def newClassPathFactory(settings: nsc.Settings): ClassPathFactory      = new ClassPathFactory(settings)
  def newClassPath(dir: AbstractFile, settings: nsc.Settings): ClassPath = ClassPathFactory.newClassPath(dir, settings)
}
