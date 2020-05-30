package com.typesafe.tools.mima.lib

import java.io.{ ByteArrayOutputStream, File, PrintStream }
import java.net.URLClassLoader

import com.typesafe.tools.mima.lib.UnitTests._

import scala.util.{ Failure, Success, Try }

/** Test running the App, using library v2. */
object AppRunTest {
  def main(args: Array[String]): Unit = TestCase.testAll(args.toList)(testAppRun)

  def testAppRun(testCase: TestCase) = {
    import testCase._
    for {
      () <- compileThem
      pending = versionedFile(baseDir / "testAppRun.pending").exists
      emptyPT = blankFile(versionedFile(baseDir / "problems.txt"))
      () <- runMain(List(outV1, outApp).map(_.jfile) ++ scalaJars) // expect at least v1 to pass, even in the "pending" case
      () <- runMain(List(outV2, outApp).map(_.jfile) ++ scalaJars) match {
        case Failure(t)  if !pending &&  emptyPT => Failure(t)
        case Success(()) if !pending && !emptyPT => Failure(new Exception("expected running App to fail"))
        case _                                   => Success(())
      }
    } yield ()
  }

  private def runMain(cp: List[File]): Try[Unit] = {
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
}
