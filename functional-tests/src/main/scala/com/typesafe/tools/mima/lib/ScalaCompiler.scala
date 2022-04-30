package com.typesafe.tools.mima.lib

import java.net.URLClassLoader

import scala.util.{Failure, Success, Try}

import coursier._

final class ScalaCompiler(val version: String) {
  val isScala3 = version.startsWith("3.")

  val name = if (isScala3) ModuleName(s"scala3-compiler_3") else name"scala-compiler"
  val jars = Coursier.fetch(Dependency(Module(org"org.scala-lang", name), version))

  val classLoader = new URLClassLoader(jars.toArray.map(_.toURI.toURL), parentClassLoader())

  def parentClassLoader() = Try { // needed for nsc 2.11 on JDK 11
    classOf[ClassLoader].getMethod("getPlatformClassLoader").invoke(null).asInstanceOf[ClassLoader]
  }.getOrElse(null) // on JDK 8 javaBootClassPath works

  def compile(args: Seq[String]): Try[Unit] = {
    import scala.language.reflectiveCalls
    val clsName = if (isScala3) "dotty.tools.dotc.Main$" else "scala.tools.nsc.Main$"
    val cls     = classLoader.loadClass(clsName)
    type Main     = { def process(args: Array[String]): Any; def reporter: Reporter }
    type Reporter = { def hasErrors: Boolean }
    val m = cls.getField("MODULE$").get(null).asInstanceOf[Main]
    Try {
      val success = m.process(args.toArray) match {
        case b: Boolean => b
        case null       => !m.reporter.hasErrors // nsc 2.11
        case x          => !x.asInstanceOf[Reporter].hasErrors // dotc
      }
      if (success) Success(()) else Failure(new Exception("scalac failed"))
    }.flatten
  }
}
