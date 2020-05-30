package com.typesafe.tools.mima.lib

import java.net.URLClassLoader
import scala.util.{ Failure, Success, Try }

final class ScalaCompiler(val version: String) {
  import coursier._
  val jars = Coursier.fetch(Dependency(mod"org.scala-lang:scala-compiler", version))

  val classLoader = {
    val parent = try { // needed for nsc 2.11 on JDK 11
      classOf[ClassLoader].getMethod("getPlatformClassLoader").invoke(null).asInstanceOf[ClassLoader]
    } catch {
      case e if !e.isInstanceOf[VirtualMachineError] => null // on JDK 8 javaBootClassPath works
    }
    new URLClassLoader(jars.toArray.map(_.toURI.toURL), parent)
  }

  def compile(args: Seq[String]): Try[Unit] = {
    import scala.language.reflectiveCalls
    val c = classLoader.loadClass("scala.tools.nsc.Main$")
    val m = c.getField("MODULE$").get(null).asInstanceOf[{ def process(args: Array[String]): Any }]
    Try(m.process(args.toArray) match {
      case b: Boolean => b
      case null       => // nsc 2.11
        val reporter = m.asInstanceOf[{ def reporter: { def hasErrors: Boolean } }].reporter
        !reporter.hasErrors || { reporter.asInstanceOf[{ def flush(): Unit }].flush(); false }
    }).flatMap { success =>
      if (success) Success(())
      else Failure(new Exception("scalac failed"))
    }
  }
}
