package com.typesafe.tools.mima.lib

import java.net.URI
import javax.tools._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.util.{ BatchSourceFile, SourceFile }
import scala.reflect.io.{ Directory, Path, PlainFile }
import scala.util.{ Failure, Success, Try }

import com.typesafe.tools.mima.core.ClassPath

object TestCase {
  val scala211 = "2.11.12"
  val scala212 = "2.12.11"
  val scala213 = "2.13.2"
  val hostScalaVersion = scala.util.Properties.versionNumberString

  def testAll(argv: List[String])(test: TestCase => Try[Unit]): Unit =
    TestPrinter.testAll(fromArgs(argv))(tc => s"${tc.scalaBinaryVersion} / ${tc.name}")(test)

  def fromArgs(argv: List[String]): List[TestCase] = {
    val Conf(svs, dirs0) = go(argv, Conf(Nil, Nil))
    val dirs = if (dirs0.nonEmpty) dirs0.reverse else
      Directory("functional-tests/src/test").dirs
        .filter(_.files.exists(_.name == "problems.txt"))
        .toSeq.sortBy(_.path)
    val scalaCompilers = (if (svs.isEmpty) List(hostScalaVersion) else svs.reverse).map(new ScalaCompiler(_))
    val javaCompiler = ToolProvider.getSystemJavaCompiler
    for (sc <- scalaCompilers; dir <- dirs) yield new TestCase(dir, sc, javaCompiler)
  }

  final case class Conf(scalaVersions: List[String], dirs: List[Directory])

  @tailrec private def go(argv: List[String], conf: Conf): Conf = argv match {
    case "-213" :: xs                  => go(xs, conf.copy(scalaVersions = scala213 :: conf.scalaVersions))
    case "-212" :: xs                  => go(xs, conf.copy(scalaVersions = scala212 :: conf.scalaVersions))
    case "-211" :: xs                  => go(xs, conf.copy(scalaVersions = scala211 :: conf.scalaVersions))
    case "--scala-version" :: sv :: xs => go(xs, conf.copy(scalaVersions = sv :: conf.scalaVersions))
    case "--cross" :: xs               => go(xs, conf.copy(scalaVersions = List(scala211, scala212, scala213)))
    case s :: xs                       => go(xs, conf.copy(dirs = testDir(s) ::: conf.dirs))
    case Nil                           => conf
  }

  def testDir(s: String) = {
    val base = Directory("functional-tests/src/test")
    base.dirs.find(_.name == s) match {
      case Some(d) => List(d)
      case _       => base.dirs.filter(_.name.contains(s)).toList.sortBy(_.path) match {
        case Nil  => sys.error(s"No such directory: ${base / s}")
        case dirs => dirs
      }
    }
  }
}

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
  List(outV1, outV2, outApp).foreach { out =>
    if (out.exists)
      assert(out.deleteRecursively(), s"failed to delete $out")
    out.createDirectory()
  }

  lazy val compileThem: Try[Unit] = for {
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
    if (success) Success(()) else
      Failure(new Exception(s"javac failed; ${infos.size} messages:\n${infos.mkString("\n  ", "  ", "")}"))
  }

  def versionedFile(path: Path) = {
    val p    = path.toFile
    val p211 = (p.parent / (s"${p.stripExtension}-2.11")).addExtension(p.extension).toFile
    val p212 = (p.parent / (s"${p.stripExtension}-2.12")).addExtension(p.extension).toFile
    scalaBinaryVersion match {
      case "2.11" => if (p211.exists) p211 else if (p212.exists) p212 else p
      case "2.12" => if (p212.exists) p212 else p
      case _      => p
    }
  }
}
