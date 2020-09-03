package com.typesafe.tools.mima.lib

import java.io.{ ByteArrayOutputStream, PrintStream }
import java.net.{ URI, URLClassLoader }
import javax.tools._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.util.{ BatchSourceFile, SourceFile }
import scala.reflect.io.{ Directory, Path, PlainFile }
import scala.util.{ Failure, Success, Try }

import com.typesafe.tools.mima.core.ClassPath

final class TestCase(val baseDir: Directory, val scalaCompiler: ScalaCompiler, val javaCompiler: JavaCompiler) {
  def name               = baseDir.name
  def scalaBinaryVersion = scalaCompiler.version.take(4)
  def scalaJars          = scalaCompiler.jars

  val srcV1  = (baseDir / "v1").toDirectory
  val srcV2  = (baseDir / "v2").toDirectory
  val srcApp = (baseDir / "app").toDirectory
  val outV1  = (baseDir / s"target/scala-$scalaBinaryVersion/v1-classes").toDirectory
  val outV2  = (baseDir / s"target/scala-$scalaBinaryVersion/v2-classes").toDirectory
  val outApp = (baseDir / s"target/scala-$scalaBinaryVersion/app-classes").toDirectory

  lazy val compileThem: Try[Unit] = for {
    () <- Try(List(outV1, outV2, outApp).foreach(recreateDir(_)))
    () <- compileDir(srcV1,  outV1)
    () <- compileDir(srcApp, outApp)
    () <- compileDir(srcV2,  outV2)  // run after App, to make sure App links to v1
  } yield ()

  def compileDir(srcDir: Directory, out: Directory): Try[Unit] = {
    val sourceFiles = srcDir.files.map(f => new BatchSourceFile(new PlainFile(f))).toList
    for {
      () <- compileScala(sourceFiles, out)
      () <- compileJava(sourceFiles.filter(_.isJava), out)
    } yield ()
  }

  def compileScala(sourceFiles: List[SourceFile], out: Directory): Try[Unit] = {
    if (sourceFiles.forall(_.isJava)) return Success(())
    val bootcp = ClassPath.join(scalaJars.map(_.getPath))
    val paths = sourceFiles.map(_.path)
    val args = "-bootclasspath" :: bootcp :: "-classpath" :: s"$outV1" :: "-d" :: s"$out" :: paths
    scalaCompiler.compile(args)
  }

  def compileJava(sourceFiles: List[SourceFile], out: Directory): Try[Unit] = {
    if (sourceFiles.isEmpty) return Success(())
    val cp = ClassPath.join((scalaJars :+ outV1.jfile).map(_.getPath))
    val opts = List("-classpath", cp, "-d", s"$out").asJava
    val units = sourceFiles.map { sf =>
      new SimpleJavaFileObject(new URI(s"string:///${sf.path}"), JavaFileObject.Kind.SOURCE) {
        override def getCharContent(ignoreEncodingErrors: Boolean) = sf.content
      }
    }.asJava
    val infos = new mutable.LinkedHashSet[Diagnostic[_ <: JavaFileObject]]
    val task = javaCompiler.getTask(null, null, d => infos += d, opts, null, units)
    val success = task.call() && infos.forall(_.getKind != Diagnostic.Kind.ERROR)
    if (success) Success(())
    else Failure(new Exception(s"javac failed; ${infos.size} messages:\n  ${infos.mkString("\n  ")}"))
  }

  def runMain(outLib: Directory): Try[Unit] = {
    val cp = List(outLib, outApp).map(_.jfile) ++ scalaJars
    val cl = new URLClassLoader(cp.map(_.toURI.toURL).toArray, null)
    val meth = cl.loadClass("App").getMethod("main", classOf[Array[String]])

    val printStream = new PrintStream(new ByteArrayOutputStream(), /* autoflush = */ true, "UTF-8")
    val savedOut = System.out
    val savedErr = System.err
    try {
      System.setOut(printStream)
      System.setErr(printStream)
      Console.withErr(printStream) {
        Console.withOut(printStream) {
          Try(meth.invoke(null, new Array[String](0)): Unit)
        }
      }
    } finally {
      System.setOut(savedOut)
      System.setErr(savedErr)
      printStream.close()
    }
  }

  def versionedFile(path: Path) = {
    val p    = baseDir.resolve(path).toFile
    val p211 = (p.parent / (s"${p.stripExtension}-2.11")).addExtension(p.extension).toFile
    val p212 = (p.parent / (s"${p.stripExtension}-2.12")).addExtension(p.extension).toFile
    scalaBinaryVersion match {
      case "2.11" => if (p211.exists) p211 else if (p212.exists) p212 else p
      case "2.12" => if (p212.exists) p212 else p
      case _      => p
    }
  }

  def recreateDir(dir: Directory) = {
    if (dir.exists)
      assert(dir.deleteRecursively(), s"failed to delete $dir")
    dir.createDirectory()
  }

  override def toString = s"TestCase(baseDir=${baseDir.name}, scalaVersion=${scalaCompiler.version})"
}
