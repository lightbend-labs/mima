package com.typesafe.tools.mima.lib

import com.typesafe.tools.mima.core.ClassPath

import java.io.{ByteArrayOutputStream, PrintStream}
import java.net.{URI, URLClassLoader}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.tools._
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.{Directory, Path, PlainFile}
import scala.util.{Failure, Success, Try}

final class TestCase(val baseDir: Directory, val scalaCompiler: ScalaCompiler, val javaCompiler: JavaCompiler) {
  def name               = baseDir.name
  def scalaBinaryVersion = if (scalaCompiler.isScala3) "3" else scalaCompiler.version.take(4)
  def scalaJars          = scalaCompiler.jars

  def skip: Boolean      = (baseDir / s"skip-${scalaBinaryVersion}.txt").exists

  val srcV1  = (baseDir / "v1").toDirectory
  val srcV2  = (baseDir / "v2").toDirectory
  val srcApp = (baseDir / "app").toDirectory
  val outV1  = (baseDir / s"target/scala-$scalaBinaryVersion/v1-classes").toDirectory
  val outV2  = (baseDir / s"target/scala-$scalaBinaryVersion/v2-classes").toDirectory
  val outApp = (baseDir / s"target/scala-$scalaBinaryVersion/app-classes").toDirectory

  lazy val compileBoth: Try[Unit]   = for (() <- compileV1(); () <- compileV2()) yield ()
  def compileV1(): Try[Unit]        = compileDir(srcV1, Nil, outV1)
  def compileV2(): Try[Unit]        = compileDir(srcV2, Nil, outV2)
  def compileApp(outLib: Directory) = for {
    () <- compileBoth
    () <- compileDir(srcApp, List(outLib), outApp)
  } yield ()

  def compileDir(srcDir: Directory, cp: List[Directory], out: Directory): Try[Unit] = for {
    () <- Try(recreateDir(out))
    () <- compileScala(srcDir, cp, out)
    () <- compileJava(srcDir, cp, out)
  } yield ()

  def compileScala(srcDir: Directory, cp: List[Directory], out: Directory): Try[Unit] = {
    val sourceFiles = lsSrcs(srcDir)
    if (sourceFiles.forall(_.isJava)) return Success(())
    val bootcp = ClassPath.join(scalaJars.map(_.getPath))
    val cpOpt  = if (cp.isEmpty) Nil else List("-classpath", ClassPath.join(cp.map(_.path)))
    val optsFile = baseDir / s"scalac-options-${scalaBinaryVersion}.txt"
    val testOpts = if (optsFile.exists) {
      Files.readAllLines(optsFile.jfile.toPath, StandardCharsets.UTF_8).asScala.filterNot(_.trim.startsWith("#")).toList
    } else {
      List.empty
    }
    val paths = sourceFiles.map(_.path)
    val args = "-bootclasspath" :: bootcp :: testOpts ::: cpOpt ::: "-d" :: s"$out" :: paths
    scalaCompiler.compile(args)
  }

  def compileJava(srcDir: Directory, cp: List[Directory], out: Directory): Try[Unit] = {
    val sourceFiles = lsSrcs(srcDir, _.hasExtension("java"))
    if (sourceFiles.isEmpty) return Success(())
    val cpStr = ClassPath.join((scalaJars ++ cp.map(_.jfile)).map(_.getPath))
    val opts = List("-classpath", cpStr, "-d", s"$out").asJava
    val units = sourceFiles.map { sf =>
      new SimpleJavaFileObject(new URI(s"string:///${sf.path}"), JavaFileObject.Kind.SOURCE) {
        override def getCharContent(ignoreEncodingErrors: Boolean) = java.nio.CharBuffer.wrap(sf.content)
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
          try {
            meth.invoke(null, new Array[String](0))
            Success(())
          } catch {
            case e: VirtualMachineError                 => throw e
            case e: ThreadDeath                         => throw e
            case e: InterruptedException                => throw e
            case e: scala.util.control.ControlThrowable => throw e // don't rethrow LinkageError
            case e: Throwable                           => Failure(rootCause(e))
          }
        }
      }
    } finally {
      System.setOut(savedOut)
      System.setErr(savedErr)
      printStream.close()
    }
  }

  def lsSrcs(dir: Directory, cond: Path => Boolean = _.hasExtension("scala", "java")) = {
    dir.walkFilter(cond).map(f => new BatchSourceFile(new PlainFile(f))).toList.sortBy(_.path)
  }

  def blankFile(p: Path): Boolean = p.toFile.lines().forall(_.startsWith("#"))

  def versionedFile(path: Path) = {
    val p    = baseDir.resolve(path).toFile
    val p211 = (p.parent / (s"${p.stripExtension}-2.11")).addExtension(p.extension).toFile
    val p212 = (p.parent / (s"${p.stripExtension}-2.12")).addExtension(p.extension).toFile
    val p213 = (p.parent / (s"${p.stripExtension}-2.13")).addExtension(p.extension).toFile
    val p3   = (p.parent / (s"${p.stripExtension}-3"   )).addExtension(p.extension).toFile
    scalaBinaryVersion match {
      case "2.11" => if (p211.exists) p211 else if (p212.exists) p212 else p
      case "2.12" => if (p212.exists) p212 else p
      case "2.13" => if (p213.exists) p213 else if (p212.exists) p212 else p
      case "3"    => if (p3.exists)   p3   else p
      case _      => p
    }
  }

  def recreateDir(dir: Directory): Unit = {
    if (dir.exists)
      assert(dir.deleteRecursively(), s"failed to delete $dir")
    dir.createDirectory()
    ()
  }

  @tailrec private def rootCause(x: Throwable): Throwable = x match {
    case _: ExceptionInInitializerError |
         _: java.lang.reflect.InvocationTargetException |
         _: java.lang.reflect.UndeclaredThrowableException |
         _: java.util.concurrent.ExecutionException
        if x.getCause != null =>
      rootCause(x.getCause)
    case _ => x
  }

  override def toString = s"TestCase(baseDir=${baseDir.name}, scalaVersion=${scalaCompiler.version})"
}
