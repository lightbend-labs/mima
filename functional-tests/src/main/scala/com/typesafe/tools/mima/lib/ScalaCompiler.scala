package com.typesafe.tools.mima.lib

import java.net.URLClassLoader

import scala.util.{ Failure, Success, Try }

import coursier._

final class ScalaCompiler(val version: String) {
  val jars = Coursier.fetch(Dependency(mod"org.scala-lang:scala-compiler", version))

  val classLoader = new URLClassLoader(jars.toArray.map(_.toURI.toURL), parentClassLoader())

  def parentClassLoader() = Try { // needed for nsc 2.11 on JDK 11
    classOf[ClassLoader].getMethod("getPlatformClassLoader").invoke(null).asInstanceOf[ClassLoader]
  }.getOrElse(null) // on JDK 8 javaBootClassPath works

  def compile(args: Seq[String]): Try[Unit] = {
    import scala.language.reflectiveCalls
    val cls = classLoader.loadClass("scala.tools.nsc.Main$")
    type Main     = { def process(args: Array[String]): Any; def reporter: Reporter }
    type Reporter = { def hasErrors: Boolean }
    val m = cls.getField("MODULE$").get(null).asInstanceOf[Main]
    Try {
      val success = m.process(args.toArray) match {
        case b: Boolean => b
        case null       => !m.reporter.hasErrors // nsc 2.11
      }
      if (success) Success(()) else Failure(new Exception("scalac failed"))
    }
  }
}
